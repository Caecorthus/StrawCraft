package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatheRoundParticipantLifecycleTest {
    @Test
    void vanillaDeathClearsRuntimeStateAndMirrorsLiveRoundParticipantDeathIntoWathe() {
        WatheRoundParticipantLifecycle.ParticipantState state =
                new WatheRoundParticipantLifecycle.ParticipantState(true, true, false, false);

        WatheRoundParticipantLifecycle.DeathActions actions = WatheRoundParticipantLifecycle.afterVanillaDeath(state);

        assertTrue(actions.clearRuntimeState());
        assertTrue(actions.clearDeathAttribution());
        assertTrue(actions.forwardDeathToWathe());
        assertTrue(actions.syncWatheRound());
    }

    @Test
    void vanillaDeathOnlyMarksPlayersWhoAreActiveWatheParticipants() {
        assertEquals(new WatheRoundParticipantLifecycle.DeathActions(true, true, false, false),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(false, true, false, false)));
        assertEquals(new WatheRoundParticipantLifecycle.DeathActions(true, true, false, false),
                WatheRoundParticipantLifecycle.afterVanillaDeath(
                        new WatheRoundParticipantLifecycle.ParticipantState(true, false, false, false)));
        assertEquals(new WatheRoundParticipantLifecycle.DeathActions(true, true, false, false),
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
}
