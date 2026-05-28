package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaotieSwallowPolicyTest {
    @Test
    void initializeForGameMirrorsSourceBackedThresholdAndCooldownMath() {
        TaotieSwallowPolicy.RoundState smallGame = TaotieSwallowPolicy.initializeForGame(5);
        TaotieSwallowPolicy.RoundState midGame = TaotieSwallowPolicy.initializeForGame(30);
        TaotieSwallowPolicy.RoundState largeGame = TaotieSwallowPolicy.initializeForGame(45);

        assertEquals(2, smallGame.triggerThreshold());
        assertEquals(50 * 20, smallGame.calculatedSwallowCooldownTicks());
        assertEquals(6, midGame.triggerThreshold());
        assertEquals(30 * 20, midGame.calculatedSwallowCooldownTicks());
        assertEquals(9, largeGame.triggerThreshold());
        assertEquals(20 * 20, largeGame.calculatedSwallowCooldownTicks());
        assertTrue(midGame.swallowedPlayers().isEmpty());
        assertFalse(midGame.taotieMomentActive());
        assertEquals(0, midGame.swallowCooldownTicks());
    }

    @Test
    void validatesSwallowIntentThroughServerOwnedSourceGuards() {
        assertEquals(TaotieSwallowPolicy.ValidationResult.ALLOWED,
                TaotieSwallowPolicy.validateSwallow(validSwallow()));

        assertBlocked(validSwallow().withRoundRunning(false));
        assertBlocked(validSwallow().withTaotieRole(false));
        assertBlocked(validSwallow().withTaotieAlive(false));
        assertBlocked(validSwallow().withTaotieSwallowed(true));
        assertBlocked(validSwallow().withCooldownReady(false));
        assertBlocked(validSwallow().withTargetPresent(false));
        assertBlocked(validSwallow().withSelfTarget(true));
        assertBlocked(validSwallow().withTargetAlivePlaying(false));
        assertBlocked(validSwallow().withTargetAliveSurvival(false));
        assertBlocked(validSwallow().withTargetSwallowed(true));
        assertBlocked(validSwallow().withSameWorld(false));
        assertBlocked(validSwallow().withWithinDistance(false));
        assertBlocked(validSwallow().withLineOfSight(false));
    }

    @Test
    void swallowReleaseAndTickKeepOnlyServerPolicyState() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        TaotieSwallowPolicy.RoundState state = TaotieSwallowPolicy.initializeForGame(20);

        state = TaotieSwallowPolicy.recordSwallow(state, firstTarget);
        state = TaotieSwallowPolicy.recordSwallow(state, secondTarget);

        assertEquals(Set.of(firstTarget, secondTarget), state.swallowedPlayers());
        assertEquals(state.calculatedSwallowCooldownTicks(), state.swallowCooldownTicks());

        TaotieSwallowPolicy.RoundState ticked = TaotieSwallowPolicy.tick(state);
        assertEquals(state.calculatedSwallowCooldownTicks() - 1, ticked.swallowCooldownTicks());

        TaotieSwallowPolicy.RoundState releasedOne = TaotieSwallowPolicy.releasePlayer(ticked, firstTarget);
        assertEquals(Set.of(secondTarget), releasedOne.swallowedPlayers());

        TaotieSwallowPolicy.RoundState releasedAll = TaotieSwallowPolicy.releaseAllPlayers(releasedOne);
        assertTrue(releasedAll.swallowedPlayers().isEmpty());
        assertEquals(releasedOne.swallowCooldownTicks(), releasedAll.swallowCooldownTicks());
    }

    @Test
    void momentStartsAtSourceThresholdAndCompletesOnlyAfterCountdown() {
        TaotieSwallowPolicy.RoundState state = TaotieSwallowPolicy.initializeForGame(30);

        TaotieSwallowPolicy.RoundState notStarted = TaotieSwallowPolicy.checkAndTriggerMoment(state, 7);
        assertFalse(notStarted.taotieMomentActive());

        TaotieSwallowPolicy.RoundState started = TaotieSwallowPolicy.checkAndTriggerMoment(state, 6);
        assertTrue(started.taotieMomentActive());
        assertEquals(TaotieSwallowPolicy.TAOTIE_MOMENT_DURATION_TICKS, started.taotieMomentTicks());
        assertFalse(TaotieSwallowPolicy.hasTaotieMomentCompleted(started));

        TaotieSwallowPolicy.RoundState completed = new TaotieSwallowPolicy.RoundState(
                started.swallowedPlayers(),
                started.swallowCooldownTicks(),
                true,
                0,
                started.triggerThreshold(),
                started.totalPlayersAtStart(),
                started.calculatedSwallowCooldownTicks()
        );
        assertTrue(TaotieSwallowPolicy.hasTaotieMomentCompleted(completed));
    }

    @Test
    void swallowedEveryoneRequiresEveryOtherLivingPlayerInTaotieState() {
        UUID taotie = UUID.randomUUID();
        UUID swallowed = UUID.randomUUID();
        UUID deadOutside = UUID.randomUUID();
        TaotieSwallowPolicy.RoundState state = TaotieSwallowPolicy.recordSwallow(
                TaotieSwallowPolicy.initializeForGame(3),
                swallowed
        );

        assertTrue(TaotieSwallowPolicy.hasSwallowedEveryone(taotie, state, List.of(
                new TaotieSwallowPolicy.ParticipantStatus(taotie, true),
                new TaotieSwallowPolicy.ParticipantStatus(swallowed, true),
                new TaotieSwallowPolicy.ParticipantStatus(deadOutside, false)
        )));

        assertFalse(TaotieSwallowPolicy.hasSwallowedEveryone(taotie, state, List.of(
                new TaotieSwallowPolicy.ParticipantStatus(taotie, true),
                new TaotieSwallowPolicy.ParticipantStatus(swallowed, true),
                new TaotieSwallowPolicy.ParticipantStatus(UUID.randomUUID(), true)
        )));
    }

    private static void assertBlocked(TaotieSwallowPolicy.SwallowInput input) {
        assertTrue(TaotieSwallowPolicy.validateSwallow(input).blocked());
    }

    private static TaotieSwallowPolicy.SwallowInput validSwallow() {
        return new TaotieSwallowPolicy.SwallowInput(
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true
        );
    }
}
