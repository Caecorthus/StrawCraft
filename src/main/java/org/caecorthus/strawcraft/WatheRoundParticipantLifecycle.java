package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WatheRoundParticipantLifecycle {
    private WatheRoundParticipantLifecycle() {
    }

    public static void afterVanillaDeath(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        DeathActions actions = afterVanillaDeath(participantState(player, game));
        if (actions.forwardDeathToWathe()) {
            WatheDeathReasonTracker.DeathAttribution attribution = WatheDeathReasonTracker
                    .consumeDeathAttribution(player.getUuid(), StrawDeathReasons.VANILLA_DEATH)
                    .orElseThrow();
            // Official Wathe has no vanilla-death bridge, so StrawCraft forwards the finished vanilla death once.
            // 官方 Wathe 没有原版死亡桥接，所以 StrawCraft 在原版死亡完成后只转发一次给 Wathe。
            GameFunctions.killPlayer(
                    player,
                    true,
                    attribution.killerUuid()
                            .map(killerUuid -> player.getServer().getPlayerManager().getPlayer(killerUuid))
                            .orElse(null),
                    attribution.deathReason()
            );
        }
        if (actions.clearRuntimeState()) {
            clearRuntimeState(player);
        }
        if (actions.syncWatheRound()) {
            game.sync();
        }
    }

    public static boolean shouldTrackRuntimeState(ServerPlayerEntity player, GameWorldComponent game) {
        boolean shouldTrack = shouldTrackRuntimeState(participantState(player, game));
        if (!shouldTrack) {
            clearRuntimeState(player);
        }
        return shouldTrack;
    }

    static DeathActions afterVanillaDeath(ParticipantState state) {
        boolean forwardDeathToWathe = state.roundRunning() && state.hasRole() && !state.alreadyDead();
        return new DeathActions(true, true, forwardDeathToWathe, forwardDeathToWathe);
    }

    static boolean shouldTrackRuntimeState(ParticipantState state) {
        return state.playerAlive() && state.roundRunning() && !state.alreadyDead();
    }

    private static ParticipantState participantState(ServerPlayerEntity player, GameWorldComponent game) {
        return new ParticipantState(
                game.isRunning(),
                game.getRole(player) != null,
                player.isSpectator() || player.isCreative(),
                player.isAlive()
        );
    }

    private static void clearRuntimeState(ServerPlayerEntity player) {
        TaczAmmoRefillTimers.clearPlayer(player);
        WatheDeathReasonTracker.clearDeathReason(player.getUuid());
    }

    record ParticipantState(boolean roundRunning, boolean hasRole, boolean alreadyDead, boolean playerAlive) {
    }

    record DeathActions(boolean clearRuntimeState, boolean clearDeathAttribution, boolean forwardDeathToWathe, boolean syncWatheRound) {
    }
}
