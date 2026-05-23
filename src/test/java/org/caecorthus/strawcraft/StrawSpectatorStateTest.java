package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawSpectatorStateTest {
    @Test
    void vanillaSpectatorAndCreativeCountAsWatheBaselineSpectators() {
        assertEquals(StrawSpectatorState.Kind.WATHE_BASELINE,
                StrawSpectatorState.classify(new StrawSpectatorState.Snapshot(true, false, false)));
        assertEquals(StrawSpectatorState.Kind.WATHE_BASELINE,
                StrawSpectatorState.classify(new StrawSpectatorState.Snapshot(false, true, false)));
        assertTrue(StrawSpectatorState.isWatheBaselineSpectator(true, false));
        assertTrue(StrawSpectatorState.isWatheBaselineSpectator(false, true));
    }

    @Test
    void temporaryStrawSpectatorStateStaysSeparateFromWatheBaseline() {
        assertEquals(StrawSpectatorState.Kind.STRAW_TEMPORARY,
                StrawSpectatorState.classify(new StrawSpectatorState.Snapshot(false, false, true)));
        assertFalse(StrawSpectatorState.isWatheBaselineSpectator(false, false));
    }

    @Test
    void liveParticipantsAreNeitherBaselineNorTemporarySpectators() {
        assertEquals(StrawSpectatorState.Kind.ACTIVE,
                StrawSpectatorState.classify(new StrawSpectatorState.Snapshot(false, false, false)));
    }
}
