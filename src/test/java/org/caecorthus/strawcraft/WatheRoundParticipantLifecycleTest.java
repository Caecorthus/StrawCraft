package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatheRoundParticipantLifecycleTest {
    private static final Identifier OVERWORLD = Identifier.of("minecraft", "overworld");

    @Test
    void vanillaDeathClearsRuntimeStateAndMirrorsLiveRoundParticipantDeathIntoWathe() {
        WatheRoundParticipantLifecycle.ParticipantState state =
                new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false);

        WatheRoundParticipantLifecycle.DeathActions actions = WatheRoundParticipantLifecycle.afterVanillaDeath(state);

        assertTrue(actions.clearRuntimeState());
        assertTrue(actions.clearDeathAttribution());
        assertTrue(actions.forwardDeathToWathe());
        assertTrue(actions.publishOfficialDeathCompletion());
        assertTrue(actions.syncWatheRound());
    }

    @Test
    void vanillaDeathOnlyMarksPlayersWhoAreActiveWatheParticipants() {
        assertEquals(new WatheRoundParticipantLifecycle.DeathActions(true, true, false, false, false),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(false, true, false, false)));
        assertEquals(new WatheRoundParticipantLifecycle.DeathActions(true, true, false, false, false),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, false, false, false)));
        assertEquals(new WatheRoundParticipantLifecycle.DeathActions(true, true, false, false, false),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, true, false)));
    }

    @Test
    void runtimeStateTracksOnlyAliveLiveRoundParticipants() {
        assertTrue(WatheRoundParticipantLifecycle.shouldTrackRuntimeState(
                new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, true)));
        assertFalse(WatheRoundParticipantLifecycle.shouldTrackRuntimeState(
                new WatheRoundParticipantLifecycle.ParticipantState(true, true, true, true)));
        assertFalse(WatheRoundParticipantLifecycle.shouldTrackRuntimeState(
                new WatheRoundParticipantLifecycle.ParticipantState(false, true, false, true)));
        assertFalse(WatheRoundParticipantLifecycle.shouldTrackRuntimeState(
                new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)));
    }

    @Test
    void officialDeathCompletionContextKeepsWatheBodyAndSpectatorOwnershipStable() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        long gameTime = 99L;

        StrawDeathEvents.OfficialDeathContext context = WatheRoundParticipantLifecycle.officialDeathContext(
                victim,
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.POISON, Optional.of(killer), true),
                gameTime
        );

        assertEquals(victim, context.victimUuid());
        assertEquals(Optional.of(killer), context.killerUuid());
        assertTrue(context.indirectAttribution());
        assertEquals(GameConstants.DeathReasons.POISON, context.deathReason());
        assertEquals(gameTime, context.gameTime());
        assertTrue(context.spawnBodyRequested());
        assertTrue(context.watheBaselineOwnsBodyAndSpectator());
    }

    @Test
    void officialForwardForCompletedVanillaDeathCannotBeCancelledByVanillaHealthBridge() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        WatheDeathReasonTracker.DeathAttribution attribution =
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.GUN, Optional.of(killer), false);

        WatheRoundParticipantLifecycle.OfficialDeathForward forward =
                WatheRoundParticipantLifecycle.officialDeathForward(attribution);
        VanillaHealthBridge.KillRequestPlan bridgePlan =
                VanillaHealthBridge.planKillRequest(forward.deathReason(), false, forward.killerForwarded());
        StrawDeathEvents.OfficialDeathContext context =
                WatheRoundParticipantLifecycle.officialDeathContext(victim, attribution, 100L);

        assertEquals(StrawDeathReasons.VANILLA_DEATH, forward.deathReason());
        assertFalse(forward.killerForwarded());
        assertFalse(bridgePlan.cancelsWatheKill());
        assertEquals(GameConstants.DeathReasons.GUN, context.deathReason());
        assertEquals(Optional.of(killer), context.killerUuid());
    }

    @Test
    void blockedOfficialForwardDoesNotPublishCompletionOrPayRewards() {
        UUID victim = UUID.randomUUID();
        WatheDeathReasonTracker.DeathAttribution attribution =
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.GUN, Optional.empty(), false);
        TestLifecycleHooks hooks = new TestLifecycleHooks(attribution);
        hooks.officialBaselineCompleted = false;

        WatheRoundParticipantLifecycle.LifecycleResult result = WatheRoundParticipantLifecycle.completeVanillaDeath(
                victim,
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                hooks.asHooks()
        );

        assertEquals(1, hooks.forwarded.get());
        assertEquals(0, hooks.rewardsPaid.get());
        assertEquals(0, hooks.published.get());
        assertEquals(1, hooks.runtimeClears.get());
        assertEquals(1, hooks.syncs.get());
        assertFalse(result.officialDeathCompleted());
        assertFalse(result.rewardsPaid());
    }

    @Test
    void throwingCompletionListenerCannotSkipRuntimeCleanupOrSync() {
        WatheDeathReasonTracker.DeathAttribution attribution =
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.GUN, Optional.empty(), false);
        TestLifecycleHooks hooks = new TestLifecycleHooks(attribution);
        hooks.publishFailure = new IllegalStateException("listener failed");

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> WatheRoundParticipantLifecycle.completeVanillaDeath(
                UUID.randomUUID(),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                hooks.asHooks()
        ));

        assertEquals("listener failed", failure.getMessage());
        assertEquals(1, hooks.rewardsPaid.get());
        assertEquals(1, hooks.runtimeClears.get());
        assertEquals(1, hooks.syncs.get());
        assertEquals(1, hooks.published.get());
    }

    @Test
    void throwingOfficialForwardStillClearsDeathAttribution() {
        WatheDeathReasonTracker.DeathAttribution attribution =
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.GUN, Optional.empty(), false);
        TestLifecycleHooks hooks = new TestLifecycleHooks(attribution);
        hooks.forwardFailure = new IllegalStateException("forward failed");

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> WatheRoundParticipantLifecycle.completeVanillaDeath(
                UUID.randomUUID(),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                hooks.asHooks()
        ));

        assertEquals("forward failed", failure.getMessage());
        assertEquals(1, hooks.forwarded.get());
        assertEquals(1, hooks.deathAttributionClears.get());
        assertEquals(1, hooks.runtimeClears.get());
        assertEquals(1, hooks.syncs.get());
        assertEquals(0, hooks.rewardsPaid.get());
        assertEquals(0, hooks.published.get());
    }

    @Test
    void completedOfficialForwardPublishesOriginalMetadataAndForwardsVanillaDeathToWathe() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        WatheDeathReasonTracker.DeathAttribution attribution =
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.GUN, Optional.of(killer), false);
        TestLifecycleHooks hooks = new TestLifecycleHooks(attribution);
        hooks.gameTime = 123L;

        WatheRoundParticipantLifecycle.LifecycleResult result = WatheRoundParticipantLifecycle.completeVanillaDeath(
                victim,
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                hooks.asHooks()
        );

        StrawDeathEvents.OfficialDeathContext published = hooks.publishedContext.get();
        assertNotNull(published);
        assertEquals(StrawDeathReasons.VANILLA_DEATH, hooks.forwardedReason.get());
        assertFalse(hooks.forwardedKiller.get());
        assertEquals(GameConstants.DeathReasons.GUN, published.deathReason());
        assertEquals(Optional.of(killer), published.killerUuid());
        assertEquals(123L, published.gameTime());
        assertTrue(published.watheBaselineOwnsBodyAndSpectator());
        assertTrue(result.officialDeathCompleted());
        assertTrue(result.rewardsPaid());
    }

    @Test
    void completedOfficialForwardClearsDeathAttributionAfterForwarding() {
        WatheDeathReasonTracker.DeathAttribution attribution =
                new WatheDeathReasonTracker.DeathAttribution(GameConstants.DeathReasons.GUN, Optional.empty(), false);
        TestLifecycleHooks hooks = new TestLifecycleHooks(attribution);
        List<String> events = new ArrayList<>();
        hooks.onForward = () -> events.add("forward");
        hooks.onClearDeathAttribution = () -> events.add("clear-attribution");

        WatheRoundParticipantLifecycle.completeVanillaDeath(
                UUID.randomUUID(),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                hooks.asHooks()
        );

        assertEquals(List.of("forward", "clear-attribution"), events);
        assertEquals(1, hooks.deathAttributionClears.get());
    }

    @Test
    void secondLifecycleDeathWithoutNewAttributionDoesNotReusePreviousKillerOrReason() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        WatheDeathReasonTracker.rememberDeathAttribution(victim, GameConstants.DeathReasons.GUN, killer);
        AtomicReference<StrawDeathEvents.OfficialDeathContext> firstDeath = new AtomicReference<>();
        AtomicReference<StrawDeathEvents.OfficialDeathContext> secondDeath = new AtomicReference<>();

        WatheRoundParticipantLifecycle.completeVanillaDeath(
                victim,
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                trackerBackedHooks(victim, firstDeath)
        );
        WatheRoundParticipantLifecycle.completeVanillaDeath(
                victim,
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false)),
                trackerBackedHooks(victim, secondDeath)
        );

        assertEquals(GameConstants.DeathReasons.GUN, firstDeath.get().deathReason());
        assertEquals(Optional.of(killer), firstDeath.get().killerUuid());
        assertEquals(StrawDeathReasons.VANILLA_DEATH, secondDeath.get().deathReason());
        assertEquals(Optional.empty(), secondDeath.get().killerUuid());
    }

    private static WatheRoundParticipantLifecycle.LifecycleHooks trackerBackedHooks(
            UUID victim,
            AtomicReference<StrawDeathEvents.OfficialDeathContext> publishedContext
    ) {
        return new WatheRoundParticipantLifecycle.LifecycleHooks(
                () -> WatheDeathReasonTracker.deathAttribution(victim, StrawDeathReasons.VANILLA_DEATH).orElseThrow(),
                ignored -> {
                },
                ignored -> {
                },
                () -> true,
                publishedContext::set,
                () -> WatheDeathReasonTracker.clearDeathAttribution(victim),
                () -> {
                },
                () -> {
                },
                () -> OVERWORLD,
                () -> 100L
        );
    }

    private static final class TestLifecycleHooks {
        private final WatheDeathReasonTracker.DeathAttribution attribution;
        private final AtomicInteger forwarded = new AtomicInteger();
        private final AtomicInteger rewardsPaid = new AtomicInteger();
        private final AtomicInteger published = new AtomicInteger();
        private final AtomicInteger runtimeClears = new AtomicInteger();
        private final AtomicInteger deathAttributionClears = new AtomicInteger();
        private final AtomicInteger syncs = new AtomicInteger();
        private final AtomicReference<Identifier> forwardedReason = new AtomicReference<>();
        private final AtomicReference<Boolean> forwardedKiller = new AtomicReference<>();
        private final AtomicReference<StrawDeathEvents.OfficialDeathContext> publishedContext = new AtomicReference<>();
        private boolean officialBaselineCompleted = true;
        private long gameTime = 99L;
        private RuntimeException forwardFailure;
        private RuntimeException publishFailure;
        private Runnable onForward = () -> {
        };
        private Runnable onClearDeathAttribution = () -> {
        };

        private TestLifecycleHooks(WatheDeathReasonTracker.DeathAttribution attribution) {
            this.attribution = attribution;
        }

        private WatheRoundParticipantLifecycle.LifecycleHooks asHooks() {
            return new WatheRoundParticipantLifecycle.LifecycleHooks(
                    () -> attribution,
                    ignored -> rewardsPaid.incrementAndGet(),
                    forward -> {
                        forwarded.incrementAndGet();
                        forwardedReason.set(forward.deathReason());
                        forwardedKiller.set(forward.killerForwarded());
                        onForward.run();
                        if (forwardFailure != null) {
                            throw forwardFailure;
                        }
                    },
                    () -> officialBaselineCompleted,
                    context -> {
                        published.incrementAndGet();
                        publishedContext.set(context);
                        if (publishFailure != null) {
                            throw publishFailure;
                        }
                    },
                    () -> {
                        deathAttributionClears.incrementAndGet();
                        onClearDeathAttribution.run();
                    },
                    runtimeClears::incrementAndGet,
                    syncs::incrementAndGet,
                    () -> OVERWORLD,
                    () -> gameTime
            );
        }
    }
}
