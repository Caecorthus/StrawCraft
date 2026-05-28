package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorruptCopMomentPolicyTest {
    private static final UUID CORRUPT_COP = new UUID(0, 1);
    private static final UUID PASSENGER = new UUID(0, 2);
    private static final UUID KILLER = new UUID(0, 3);

    @Test
    void resetStoresSourceThresholdAndKeepsMomentInactive() {
        NoellesRoleState state = new NoellesRoleState();

        CorruptCopMomentPolicy.resetRoundState(state, 10);

        assertEquals(2, CorruptCopMomentPolicy.triggerThreshold(state));
        assertFalse(CorruptCopMomentPolicy.isMomentActive(state));
    }

    @Test
    void participantResetClearsMomentStateBetweenAssignments() {
        NoellesRoleState state = new NoellesRoleState();
        CorruptCopMomentPolicy.resetRoundState(state, 10);
        CorruptCopMomentPolicy.checkAndTriggerMoment(state, 2);
        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                CorruptCopMomentPolicy.CORRUPT_COP_ROLE,
                CorruptCopMomentPolicy.LAST_STAND_TRIGGER,
                java.util.Optional.empty(),
                200L
        ));

        CorruptCopMomentPolicy.resetParticipantState(state);

        assertEquals(0, CorruptCopMomentPolicy.triggerThreshold(state));
        assertFalse(CorruptCopMomentPolicy.isMomentActive(state));
        assertTrue(state.neutralWinClaim(CorruptCopMomentPolicy.CORRUPT_COP_ROLE).isEmpty());
    }

    @Test
    void momentTriggersOnlyAtMinimumTwoAliveWithinThreshold() {
        NoellesRoleState state = new NoellesRoleState();
        CorruptCopMomentPolicy.resetRoundState(state, 10);

        assertFalse(CorruptCopMomentPolicy.checkAndTriggerMoment(state, 3));
        assertFalse(CorruptCopMomentPolicy.isMomentActive(state));

        assertTrue(CorruptCopMomentPolicy.checkAndTriggerMoment(state, 2));
        assertTrue(CorruptCopMomentPolicy.isMomentActive(state));

        assertFalse(CorruptCopMomentPolicy.checkAndTriggerMoment(state, 2));
    }

    @Test
    void smallRoundsDoNotTriggerMomentFromOnePlayerThreshold() {
        NoellesRoleState state = new NoellesRoleState();
        CorruptCopMomentPolicy.resetRoundState(state, 9);

        assertEquals(1, CorruptCopMomentPolicy.triggerThreshold(state));
        assertFalse(CorruptCopMomentPolicy.checkAndTriggerMoment(state, 1));
        assertFalse(CorruptCopMomentPolicy.isMomentActive(state));
    }

    @Test
    void activeMomentCancelsOnlyAssassinationDeathRequests() {
        NoellesRoleState state = new NoellesRoleState();
        CorruptCopMomentPolicy.resetRoundState(state, 10);
        CorruptCopMomentPolicy.checkAndTriggerMoment(state, 2);

        assertTrue(CorruptCopMomentPolicy.beforeKill(state, StrawDeathReasons.ASSASSINATED).cancelWatheKill());
        assertFalse(CorruptCopMomentPolicy.beforeKill(state, dev.doctor4t.wathe.game.GameConstants.DeathReasons.GUN)
                .cancelWatheKill());

        CorruptCopMomentPolicy.endMoment(state);
        assertFalse(CorruptCopMomentPolicy.beforeKill(state, StrawDeathReasons.ASSASSINATED).cancelWatheKill());
    }

    @Test
    void sourceGunCooldownIsExposedButInactiveStateDoesNotOverride() {
        NoellesRoleState state = new NoellesRoleState();
        CorruptCopMomentPolicy.resetRoundState(state, 10);

        assertEquals(-1, CorruptCopMomentPolicy.gunCooldownTicks(state));

        CorruptCopMomentPolicy.checkAndTriggerMoment(state, 2);

        assertEquals(40, CorruptCopMomentPolicy.gunCooldownTicks(state));
    }

    @Test
    void winFoundationBlocksDefaultPassengerOrKillerWinWhileLivingCorruptCopExists() {
        CorruptCopMomentPolicy.WinDecision passengerDecision = CorruptCopMomentPolicy.evaluateWin(
                List.of(
                        participant(CORRUPT_COP, true, true, true),
                        participant(PASSENGER, true, true, false),
                        participant(KILLER, true, false, false)
                ),
                CorruptCopMomentPolicy.DefaultWin.PASSENGERS
        );

        assertEquals(CorruptCopMomentPolicy.WinDecision.BLOCK_DEFAULT, passengerDecision);
        assertEquals(
                CorruptCopMomentPolicy.WinDecision.BLOCK_DEFAULT,
                CorruptCopMomentPolicy.evaluateWin(
                        List.of(participant(CORRUPT_COP, true, true, true), participant(KILLER, true, true, false)),
                        CorruptCopMomentPolicy.DefaultWin.KILLERS
                )
        );
    }

    @Test
    void winFoundationDetectsCorruptCopAloneAlive() {
        assertEquals(
                CorruptCopMomentPolicy.WinDecision.NEUTRAL_WIN,
                CorruptCopMomentPolicy.evaluateWin(
                        List.of(
                                participant(CORRUPT_COP, true, true, true),
                                participant(PASSENGER, true, false, false)
                        ),
                        CorruptCopMomentPolicy.DefaultWin.NONE
                )
        );
    }

    private static CorruptCopMomentPolicy.Participant participant(
            UUID uuid,
            boolean assigned,
            boolean alive,
            boolean corruptCop
    ) {
        return new CorruptCopMomentPolicy.Participant(uuid, assigned, alive, corruptCop);
    }
}
