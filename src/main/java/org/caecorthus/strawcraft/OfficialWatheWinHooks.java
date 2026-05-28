package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawWinEvents;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class OfficialWatheWinHooks {
    private OfficialWatheWinHooks() {
    }

    public static Decision evaluate(
            ServerWorld world,
            GameWorldComponent game,
            GameFunctions.WinStatus winStatus
    ) {
        StrawWinEvents.WinContribution contribution = StrawWinEvents.collect(new StrawWinEvents.WinContext(
                world.getRegistryKey().getValue(),
                defaultWinFor(winStatus),
                participants(world, game),
                world.getTime()
        ));
        if (contribution.replacementDefaultWin().isPresent()) {
            recordExtraWinners(world, contribution);
            return Decision.replace(winStatusFor(contribution.replacementDefaultWin().orElseThrow()));
        }
        if (contribution.suppressDefaultWin()) {
            return Decision.suppress();
        }
        return Decision.pass();
    }

    private static List<StrawWinEvents.Participant> participants(ServerWorld world, GameWorldComponent game) {
        return game.getRoles().entrySet().stream()
                .map(entry -> participant(world, entry))
                .toList();
    }

    private static StrawWinEvents.Participant participant(ServerWorld world, Map.Entry<UUID, Role> entry) {
        UUID playerUuid = entry.getKey();
        PlayerEntity player = world.getPlayerByUuid(playerUuid);
        Role role = entry.getValue();
        return new StrawWinEvents.Participant(
                playerUuid,
                role != null,
                player != null && GameFunctions.isPlayerAliveAndSurvival(player),
                Optional.ofNullable(role).map(Role::identifier)
        );
    }

    private static void recordExtraWinners(ServerWorld world, StrawWinEvents.WinContribution contribution) {
        for (StrawWinEvents.ExtraWinner winner : contribution.extraWinners()) {
            PlayerEntity player = world.getPlayerByUuid(winner.playerUuid());
            if (player == null) {
                continue;
            }
            NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(player);
            if (state.neutralWinClaim(winner.roleId()).isPresent()) {
                continue;
            }
            state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                    winner.roleId(),
                    winner.triggerId(),
                    Optional.empty(),
                    world.getTime()
            ));
        }
    }

    private static StrawWinEvents.DefaultWin defaultWinFor(GameFunctions.WinStatus winStatus) {
        return switch (winStatus) {
            case NONE -> StrawWinEvents.DefaultWin.NONE;
            case KILLERS -> StrawWinEvents.DefaultWin.KILLERS;
            case PASSENGERS -> StrawWinEvents.DefaultWin.PASSENGERS;
            case TIME -> StrawWinEvents.DefaultWin.TIME;
            case LOOSE_END -> StrawWinEvents.DefaultWin.LOOSE_END;
        };
    }

    private static GameFunctions.WinStatus winStatusFor(StrawWinEvents.DefaultWin defaultWin) {
        return switch (defaultWin) {
            case NONE -> GameFunctions.WinStatus.NONE;
            case KILLERS -> GameFunctions.WinStatus.KILLERS;
            case PASSENGERS -> GameFunctions.WinStatus.PASSENGERS;
            case TIME -> GameFunctions.WinStatus.TIME;
            case LOOSE_END -> GameFunctions.WinStatus.LOOSE_END;
        };
    }

    public enum Action {
        PASS,
        SUPPRESS_DEFAULT,
        REPLACE_DEFAULT
    }

    public record Decision(Action action, GameFunctions.WinStatus replacementWinStatus) {
        public Decision {
            java.util.Objects.requireNonNull(action, "action");
        }

        static Decision pass() {
            return new Decision(Action.PASS, GameFunctions.WinStatus.NONE);
        }

        static Decision suppress() {
            return new Decision(Action.SUPPRESS_DEFAULT, GameFunctions.WinStatus.NONE);
        }

        static Decision replace(GameFunctions.WinStatus replacementWinStatus) {
            return new Decision(Action.REPLACE_DEFAULT, replacementWinStatus);
        }
    }
}
