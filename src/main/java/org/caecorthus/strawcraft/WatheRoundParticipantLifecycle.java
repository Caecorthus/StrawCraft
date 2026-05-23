package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class WatheRoundParticipantLifecycle {
    private WatheRoundParticipantLifecycle() {
    }

    public static void afterVanillaDeath(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        DeathActions actions = afterVanillaDeath(participantState(player, game));
        completeVanillaDeath(
                player.getUuid(),
                actions,
                new LifecycleHooks(
                        () -> WatheDeathReasonTracker
                                .consumeDeathAttribution(player.getUuid(), StrawDeathReasons.VANILLA_DEATH)
                                .orElseThrow(),
                        attribution -> KillRewardPayout.payoutVanillaDeath(player, game, attribution),
                        forward -> {
                            // Official Wathe has no vanilla-death bridge, so StrawCraft forwards the finished vanilla death once.
                            // The killer is deliberately null because StrawCraft pays rewards only after this baseline completes.
                            // 官方 Wathe 没有原版死亡桥接，所以 StrawCraft 在原版死亡完成后只转发一次给 Wathe。
                            // 这里故意传 null killer；StrawCraft 只在官方 baseline 完成后再执行奖励，避免 Wathe 再发一次主奖励。
                            GameFunctions.killPlayer(
                                    player,
                                    true,
                                    null,
                                    forward.deathReason()
                            );
                        },
                        () -> officialDeathBaselineCompleted(player),
                        context -> StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(context),
                        () -> clearRuntimeState(player),
                        game::sync,
                        () -> player.getWorld().getTime()
                )
        );
    }

    static LifecycleResult completeVanillaDeath(UUID victimUuid, DeathActions actions, LifecycleHooks hooks) {
        Objects.requireNonNull(victimUuid, "victimUuid");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(hooks, "hooks");

        boolean officialDeathCompleted = false;
        boolean rewardsPaid = false;
        StrawDeathEvents.OfficialDeathContext completionContext = null;
        try {
            if (actions.forwardDeathToWathe()) {
                WatheDeathReasonTracker.DeathAttribution attribution = hooks.consumeDeathAttribution().get();
                OfficialDeathForward forward = officialDeathForward(attribution);
                hooks.forwardOfficialDeath().accept(forward);

                // Wathe may return early, for example when psycho armour absorbs the kill.
                // Wathe 可能提前返回，例如 psycho 护甲吸收击杀时不会进入尸体/旁观 baseline。
                officialDeathCompleted = actions.publishOfficialDeathCompletion()
                        && hooks.officialDeathBaselineCompleted().getAsBoolean();
                if (officialDeathCompleted) {
                    hooks.payRewards().accept(attribution);
                    rewardsPaid = true;
                    completionContext = officialDeathContext(victimUuid, attribution, hooks.gameTime().getAsLong());
                }
            }
        } finally {
            if (actions.clearRuntimeState()) {
                hooks.clearRuntimeState().run();
            }
            if (actions.syncWatheRound()) {
                hooks.syncWatheRound().run();
            }
        }

        if (completionContext != null) {
            hooks.publishOfficialDeathCompletion().accept(completionContext);
        }
        return new LifecycleResult(officialDeathCompleted, rewardsPaid);
    }

    private static boolean officialDeathBaselineCompleted(ServerPlayerEntity player) {
        return GameFunctions.isPlayerSpectatingOrCreative(player);
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
        return new DeathActions(true, true, forwardDeathToWathe, forwardDeathToWathe, forwardDeathToWathe);
    }

    static boolean shouldTrackRuntimeState(ParticipantState state) {
        return state.playerAlive() && state.roundRunning() && !state.alreadyDead();
    }

    static StrawDeathEvents.OfficialDeathContext officialDeathContext(
            UUID victimUuid,
            WatheDeathReasonTracker.DeathAttribution attribution,
            long gameTime
    ) {
        return new StrawDeathEvents.OfficialDeathContext(
                victimUuid,
                attribution.killerUuid(),
                attribution.indirect(),
                attribution.deathReason(),
                gameTime,
                true,
                true
        );
    }

    static OfficialDeathForward officialDeathForward(WatheDeathReasonTracker.DeathAttribution attribution) {
        // StrawCraft metadata keeps the original reason; Wathe receives a non-converting reason for baseline death work.
        // StrawCraft 元数据保留原始死因；Wathe 收到不会再次转成原版伤害的死因，只负责尸体和旁观流程。
        return new OfficialDeathForward(StrawDeathReasons.VANILLA_DEATH, false);
    }

    private static ParticipantState participantState(ServerPlayerEntity player, GameWorldComponent game) {
        return new ParticipantState(
                game.isRunning(),
                game.getRole(player) != null,
                StrawSpectatorState.isWatheBaselineSpectator(player.isSpectator(), player.isCreative()),
                player.isAlive()
        );
    }

    private static void clearRuntimeState(ServerPlayerEntity player) {
        TaczAmmoRefillTimers.clearPlayer(player);
        WatheDeathReasonTracker.clearDeathReason(player.getUuid());
    }

    record ParticipantState(boolean roundRunning, boolean hasRole, boolean alreadyDead, boolean playerAlive) {
    }

    record DeathActions(
            boolean clearRuntimeState,
            boolean clearDeathAttribution,
            boolean forwardDeathToWathe,
            boolean syncWatheRound,
            boolean publishOfficialDeathCompletion
    ) {
    }

    record OfficialDeathForward(Identifier deathReason, boolean killerForwarded) {
    }

    record LifecycleHooks(
            Supplier<WatheDeathReasonTracker.DeathAttribution> consumeDeathAttribution,
            Consumer<WatheDeathReasonTracker.DeathAttribution> payRewards,
            Consumer<OfficialDeathForward> forwardOfficialDeath,
            BooleanSupplier officialDeathBaselineCompleted,
            Consumer<StrawDeathEvents.OfficialDeathContext> publishOfficialDeathCompletion,
            Runnable clearRuntimeState,
            Runnable syncWatheRound,
            LongSupplier gameTime
    ) {
    }

    record LifecycleResult(boolean officialDeathCompleted, boolean rewardsPaid) {
    }
}
