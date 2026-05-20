package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WatheRoundParticipantLifecycle {
    private WatheRoundParticipantLifecycle() {
    }

    public static void afterVanillaDeath(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        DeathActions actions = afterVanillaDeath(participantState(player, game));
        if (actions.clearRuntimeState()) {
            clearRuntimeState(player);
        }
        if (actions.markDeadInWathe()) {
            // Keep Wathe's win-condition bookkeeping aware of vanilla deaths.
            game.markPlayerDead(player.getUuid());
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
        boolean markDeadInWathe = state.roundRunning() && state.hasRole() && !state.alreadyDead();
        return new DeathActions(true, true, markDeadInWathe, markDeadInWathe);
    }

    static boolean shouldTrackRuntimeState(ParticipantState state) {
        return state.playerAlive() && state.roundRunning() && !state.alreadyDead();
    }

    private static ParticipantState participantState(ServerPlayerEntity player, GameWorldComponent game) {
        return new ParticipantState(
                game.isRunning(),
                game.hasAnyRole(player.getUuid()),
                game.isPlayerDead(player.getUuid()),
                player.isAlive()
        );
    }

    private static void clearRuntimeState(ServerPlayerEntity player) {
        TaczAmmoRefillTimers.clearPlayer(player);
        WatheDeathReasonTracker.clearDeathReason(player.getUuid());
    }

    record ParticipantState(boolean roundRunning, boolean hasRole, boolean alreadyDead, boolean playerAlive) {
    }

    record DeathActions(boolean clearRuntimeState, boolean clearDeathAttribution, boolean markDeadInWathe, boolean syncWatheRound) {
    }
}
