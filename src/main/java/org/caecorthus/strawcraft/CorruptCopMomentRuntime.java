package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawKillEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CorruptCopMomentRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private CorruptCopMomentRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(CorruptCopMomentRuntime::tickServer);
        StrawKillEvents.BEFORE_KILL.register(CorruptCopMomentRuntime::beforeKill);
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(CorruptCopMomentRuntime::handleRoleDeath);
        StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register(CorruptCopMomentRuntime::collectWinContributions);
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getTime() % 20 != 0) {
                continue;
            }
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!game.isRunning()) {
            return;
        }

        Observation observation = observe(world, game);
        for (ServerPlayerEntity corruptCop : observation.livingCorruptCops()) {
            NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(corruptCop);
            if (CorruptCopMomentPolicy.checkAndTriggerMoment(state, observation.livingPlayerCount())) {
                // The server owns the moment transition; client music/HUD parity can layer on later.
                // 黑警时刻的状态转换由服务端掌控；客户端音乐和 HUD 可以后续再补。
                grantCrowbarIfMissing(corruptCop);
                announceMoment(world, corruptCop);
            }
        }
    }

    private static Observation observe(ServerWorld world, GameWorldComponent game) {
        int livingPlayerCount = 0;
        List<ServerPlayerEntity> livingCorruptCops = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            livingPlayerCount++;
            if (StrawRoleMeaning.receivesCorruptCopMoment(game.getRole(player))) {
                livingCorruptCops.add(player);
            }
        }
        return new Observation(livingPlayerCount, livingCorruptCops);
    }

    private static StrawKillEvents.KillDecision beforeKill(
            PlayerEntity victim,
            @Nullable PlayerEntity killer,
            Identifier deathReason
    ) {
        if (victim == null || victim.getWorld().isClient()) {
            return StrawKillEvents.KillDecision.pass();
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role victimRole = game.getRole(victim);
        if (!StrawRoleMeaning.receivesCorruptCopMoment(victimRole)) {
            return StrawKillEvents.KillDecision.pass();
        }
        return CorruptCopMomentPolicy.beforeKill(NoellesRoleStateComponent.KEY.get(victim), deathReason);
    }

    private static void handleRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        if (context.victimRoleId().filter(CorruptCopMomentPolicy.CORRUPT_COP_ROLE::equals).isEmpty()) {
            return;
        }
        PlayerEntity victim = context.world().getPlayerByUuid(context.official().victimUuid());
        if (victim instanceof ServerPlayerEntity) {
            CorruptCopMomentPolicy.endMoment(NoellesRoleStateComponent.KEY.get(victim));
        }
    }

    private static void collectWinContributions(
            StrawWinEvents.WinContext context,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        CorruptCopMomentPolicy.WinResult result = CorruptCopMomentPolicy.evaluateWinResult(
                context.participants().stream()
                        .map(CorruptCopMomentRuntime::participant)
                        .toList(),
                defaultWinFor(context.defaultWin())
        );
        if (result.decision() == CorruptCopMomentPolicy.WinDecision.BLOCK_DEFAULT) {
            contribution.suppressDefaultWin();
        }
        if (result.decision() == CorruptCopMomentPolicy.WinDecision.NEUTRAL_WIN) {
            result.neutralWinner().ifPresent(winner -> contribution
                    .replaceDefaultWin(StrawWinEvents.DefaultWin.LOOSE_END)
                    .addExtraWinner(
                            winner,
                            CorruptCopMomentPolicy.CORRUPT_COP_ROLE,
                            CorruptCopMomentPolicy.LAST_STAND_TRIGGER
                    ));
        }
    }

    private static CorruptCopMomentPolicy.Participant participant(StrawWinEvents.Participant participant) {
        return new CorruptCopMomentPolicy.Participant(
                participant.playerUuid(),
                participant.assigned(),
                participant.alive(),
                participant.roleId().filter(CorruptCopMomentPolicy.CORRUPT_COP_ROLE::equals).isPresent()
        );
    }

    private static CorruptCopMomentPolicy.DefaultWin defaultWinFor(StrawWinEvents.DefaultWin defaultWin) {
        return switch (defaultWin) {
            case NONE -> CorruptCopMomentPolicy.DefaultWin.NONE;
            case KILLERS -> CorruptCopMomentPolicy.DefaultWin.KILLERS;
            case PASSENGERS -> CorruptCopMomentPolicy.DefaultWin.PASSENGERS;
            case TIME -> CorruptCopMomentPolicy.DefaultWin.TIME;
            case LOOSE_END -> CorruptCopMomentPolicy.DefaultWin.LOOSE_END;
        };
    }

    private static void grantCrowbarIfMissing(ServerPlayerEntity player) {
        if (carries(player, WatheItems.CROWBAR)) {
            return;
        }
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }

    private static boolean carries(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    private static void announceMoment(ServerWorld world, ServerPlayerEntity corruptCop) {
        Text broadcast = Text.literal("Corrupt Cop moment activated.").formatted(Formatting.DARK_RED);
        Text personal = Text.literal("Corrupt Cop moment: crowbar granted; assassination immunity active.")
                .formatted(Formatting.RED);
        world.getPlayers().forEach(player -> player.sendMessage(broadcast, true));
        corruptCop.sendMessage(personal, false);
    }

    private record Observation(int livingPlayerCount, List<ServerPlayerEntity> livingCorruptCops) {
        private Observation {
            livingCorruptCops = List.copyOf(livingCorruptCops);
        }
    }
}
