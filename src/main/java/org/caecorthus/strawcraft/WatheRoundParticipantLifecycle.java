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
            // 让 Wathe 的胜负结算也知道这些由原版生命值触发的死亡。
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
        // StrawCraft lets hearts reach zero, then mirrors only valid round deaths
        // back into Wathe so corpse/spectator/win-condition logic stays Wathe-owned.
        // StrawCraft 先允许红心归零，再只把有效的正式局死亡同步回 Wathe；
        // 这样尸体、旁观者和胜负结算逻辑仍然由 Wathe 接管。
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
