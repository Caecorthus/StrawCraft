package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalMasterCountdownStateTest {
    @Test
    void startsCountdownAtThresholdAndThenDecrements() {
        SurvivalMasterCountdownState state = new SurvivalMasterCountdownState();
        FakeRoundEndSink sink = new FakeRoundEndSink();

        SurvivalMasterCountdownState.Update started = state.tick(observation(2, 3, true), sink);
        SurvivalMasterCountdownState.Update decremented = state.tick(observation(2, 3, true), sink);

        assertTrue(started.started());
        assertEquals(SurvivalMasterCountdownState.COUNTDOWN_SECONDS, started.remainingSeconds());
        assertTrue(state.running());
        assertFalse(started.completed());
        assertTrue(decremented.progress());
        assertEquals(SurvivalMasterCountdownState.COUNTDOWN_SECONDS - 1, decremented.remainingSeconds());
        assertEquals(0, sink.completions);
    }

    @Test
    void cancelsCountdownWhenSurvivalMasterDiesBeforeCompletion() {
        SurvivalMasterCountdownState state = new SurvivalMasterCountdownState();
        FakeRoundEndSink sink = new FakeRoundEndSink();
        state.tick(observation(1, 2, true), sink);

        SurvivalMasterCountdownState.Update cancelled = state.tick(observation(1, 2, false), sink);

        assertTrue(cancelled.cancelled());
        assertFalse(state.running());
        assertEquals(0, state.remainingSeconds());
        assertEquals(0, sink.completions);
    }

    @Test
    void completesOnceWhenSurvivalMasterSurvivesFullCountdown() {
        SurvivalMasterCountdownState state = new SurvivalMasterCountdownState();
        FakeRoundEndSink sink = new FakeRoundEndSink();
        state.tick(observation(1, 2, true), sink);

        SurvivalMasterCountdownState.Update update = SurvivalMasterCountdownState.Update.IDLE;
        for (int second = 0; second < SurvivalMasterCountdownState.COUNTDOWN_SECONDS; second++) {
            update = state.tick(observation(1, 2, true), sink);
        }
        state.tick(observation(1, 2, true), sink);

        assertTrue(update.completed());
        assertFalse(state.running());
        assertTrue(state.completed());
        assertEquals(1, sink.completions);
    }

    @Test
    void resetClearsRunningAndCompletedState() {
        SurvivalMasterCountdownState state = new SurvivalMasterCountdownState();
        FakeRoundEndSink sink = new FakeRoundEndSink();
        state.tick(observation(1, 2, true), sink);
        for (int second = 0; second < SurvivalMasterCountdownState.COUNTDOWN_SECONDS; second++) {
            state.tick(observation(1, 2, true), sink);
        }

        state.reset();

        assertFalse(state.running());
        assertFalse(state.completed());
        assertEquals(0, state.remainingSeconds());
    }

    private static SurvivalMasterCountdownPolicy.Observation observation(
            int startingKillerCount,
            int livingPlayerCount,
            boolean survivalMasterAlive
    ) {
        return new SurvivalMasterCountdownPolicy.Observation(
                startingKillerCount,
                livingPlayerCount,
                survivalMasterAlive
        );
    }

    private static final class FakeRoundEndSink implements SurvivalMasterCountdownState.RoundEndSink {
        private int completions;

        @Override
        public void endRoundAsPassengers() {
            completions++;
        }
    }
}
