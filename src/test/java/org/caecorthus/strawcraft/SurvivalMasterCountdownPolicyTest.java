package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalMasterCountdownPolicyTest {
    @Test
    void thresholdIsStartingKillerCountPlusOneCappedAtFour() {
        assertEquals(2, SurvivalMasterCountdownPolicy.thresholdForStartingKillers(1));
        assertEquals(3, SurvivalMasterCountdownPolicy.thresholdForStartingKillers(2));
        assertEquals(4, SurvivalMasterCountdownPolicy.thresholdForStartingKillers(3));
        assertEquals(4, SurvivalMasterCountdownPolicy.thresholdForStartingKillers(4));
    }

    @Test
    void zeroStartingKillersNeverTriggersCountdown() {
        SurvivalMasterCountdownPolicy.Observation observation =
                new SurvivalMasterCountdownPolicy.Observation(0, 1, true);

        assertEquals(0, SurvivalMasterCountdownPolicy.thresholdForStartingKillers(0));
        assertFalse(SurvivalMasterCountdownPolicy.shouldStartCountdown(observation));
    }

    @Test
    void countdownStartsOnlyWhenSurvivalMasterIsAliveAtThreshold() {
        assertTrue(SurvivalMasterCountdownPolicy.shouldStartCountdown(
                new SurvivalMasterCountdownPolicy.Observation(2, 3, true)
        ));
        assertFalse(SurvivalMasterCountdownPolicy.shouldStartCountdown(
                new SurvivalMasterCountdownPolicy.Observation(2, 4, true)
        ));
        assertFalse(SurvivalMasterCountdownPolicy.shouldStartCountdown(
                new SurvivalMasterCountdownPolicy.Observation(2, 3, false)
        ));
    }
}
