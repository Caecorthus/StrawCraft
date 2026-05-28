package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JesterWinPolicyTest {
    @Test
    void innocentGunKillAgainstJesterStartsStasisAndCancelsTheDeath() {
        UUID innocent = UUID.randomUUID();

        JesterWinPolicy.KillAttemptDecision decision = JesterWinPolicy.evaluateKillAttempt(
                killAttempt(true, false, false, Optional.of(innocent), true, GameConstants.DeathReasons.GUN)
        );

        assertEquals(JesterWinPolicy.KillAttemptAction.ENTER_STASIS, decision.action());
        assertTrue(decision.cancelWatheKill());
        assertEquals(Optional.of(innocent), decision.targetKiller());
        assertEquals(JesterWinPolicy.STASIS_TICKS, decision.stasisTicks());
    }

    @Test
    void jesterDoesNotEnterStasisForNonGunKillerShotOrPsychoMode() {
        UUID killer = UUID.randomUUID();

        assertEquals(JesterWinPolicy.KillAttemptAction.PASS, JesterWinPolicy.evaluateKillAttempt(
                killAttempt(true, false, false, Optional.of(killer), false, GameConstants.DeathReasons.GUN)
        ).action());
        assertEquals(JesterWinPolicy.KillAttemptAction.PASS, JesterWinPolicy.evaluateKillAttempt(
                killAttempt(true, false, false, Optional.of(killer), true, GameConstants.DeathReasons.GENERIC)
        ).action());
        assertEquals(JesterWinPolicy.KillAttemptAction.PASS, JesterWinPolicy.evaluateKillAttempt(
                killAttempt(true, false, true, Optional.of(killer), true, GameConstants.DeathReasons.GUN)
        ).action());
    }

    @Test
    void stasisBlocksOrdinaryDeathsButResetDeathsAreAllowedThrough() {
        JesterWinPolicy.KillAttemptDecision ordinary = JesterWinPolicy.evaluateKillAttempt(
                killAttempt(true, true, false, Optional.empty(), false, GameConstants.DeathReasons.GENERIC)
        );
        JesterWinPolicy.KillAttemptDecision escaped = JesterWinPolicy.evaluateKillAttempt(
                killAttempt(true, true, false, Optional.empty(), false, JesterWinPolicy.SPARK_ESCAPED_DEATH)
        );

        assertEquals(JesterWinPolicy.KillAttemptAction.CANCEL_STASIS_DEATH, ordinary.action());
        assertTrue(ordinary.cancelWatheKill());
        assertEquals(JesterWinPolicy.KillAttemptAction.RESET_STASIS_AND_ALLOW_DEATH, escaped.action());
        assertFalse(escaped.cancelWatheKill());
    }

    @Test
    void targetDeathCompletesJesterWinOnlyForTheTrackedKiller() {
        UUID target = UUID.randomUUID();

        assertTrue(JesterWinPolicy.targetDeathCompletesWin(Optional.of(target), target));
        assertFalse(JesterWinPolicy.targetDeathCompletesWin(Optional.of(target), UUID.randomUUID()));
        assertFalse(JesterWinPolicy.targetDeathCompletesWin(Optional.empty(), target));
    }

    @Test
    void winCheckNeutralWinsAfterTargetDeathAndBlocksDefaultWinsOnlyDuringPsychoMode() {
        assertEquals(JesterWinPolicy.WinCheckDecision.NEUTRAL_WIN, JesterWinPolicy.evaluateWinCheck(
                new JesterWinPolicy.WinCheckInput(true, true, false, StrawWinEvents.DefaultWin.PASSENGERS)
        ));
        assertEquals(JesterWinPolicy.WinCheckDecision.BLOCK_DEFAULT_WIN, JesterWinPolicy.evaluateWinCheck(
                new JesterWinPolicy.WinCheckInput(true, false, true, StrawWinEvents.DefaultWin.KILLERS)
        ));
        assertEquals(JesterWinPolicy.WinCheckDecision.BLOCK_DEFAULT_WIN, JesterWinPolicy.evaluateWinCheck(
                new JesterWinPolicy.WinCheckInput(true, false, true, StrawWinEvents.DefaultWin.PASSENGERS)
        ));
        assertEquals(JesterWinPolicy.WinCheckDecision.PASS, JesterWinPolicy.evaluateWinCheck(
                new JesterWinPolicy.WinCheckInput(true, false, true, StrawWinEvents.DefaultWin.TIME)
        ));
        assertEquals(JesterWinPolicy.WinCheckDecision.PASS, JesterWinPolicy.evaluateWinCheck(
                new JesterWinPolicy.WinCheckInput(false, true, true, StrawWinEvents.DefaultWin.PASSENGERS)
        ));
    }

    @Test
    void participantResetClearsJesterMomentStateAndNeutralClaimBetweenAssignments() {
        NoellesRoleState state = new NoellesRoleState();
        state.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                true,
                false,
                0,
                true,
                JesterRuntime.PSYCHO_TICKS,
                Optional.of(UUID.randomUUID()),
                1.0D,
                2.0D,
                3.0D
        ));
        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                JesterWinPolicy.JESTER_ROLE,
                JesterWinPolicy.TARGET_KILLED_TRIGGER,
                Optional.of(UUID.randomUUID()),
                300L
        ));

        JesterWinPolicy.resetParticipantState(state);

        assertFalse(state.jesterMomentState().hasState());
        assertTrue(state.neutralWinClaim(JesterWinPolicy.JESTER_ROLE).isEmpty());
    }

    @Test
    void runtimeContributionPromotesJesterClaimToLooseEndReplacementAndExtraWinner() {
        UUID jester = UUID.randomUUID();
        NoellesRoleState state = new NoellesRoleState();
        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                JesterWinPolicy.JESTER_ROLE,
                JesterWinPolicy.TARGET_KILLED_TRIGGER,
                Optional.of(UUID.randomUUID()),
                300L
        ));
        StrawWinEvents.WinContribution.Builder builder = StrawWinEvents.WinContribution.builder();

        JesterRuntime.collectWinContribution(jester, state, StrawWinEvents.DefaultWin.PASSENGERS, builder);

        StrawWinEvents.WinContribution contribution = builder.build();
        assertFalse(contribution.suppressDefaultWin());
        assertEquals(Optional.of(StrawWinEvents.DefaultWin.LOOSE_END), contribution.replacementDefaultWin());
        assertTrue(contribution.extraWinners().contains(new StrawWinEvents.ExtraWinner(
                jester,
                JesterWinPolicy.JESTER_ROLE,
                JesterWinPolicy.TARGET_KILLED_TRIGGER
        )));
    }

    @Test
    void psychoJesterSuppressesPassengerAndKillerDefaultWinsButNotTime() {
        UUID jester = UUID.randomUUID();
        NoellesRoleState state = new NoellesRoleState();
        state.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                false,
                false,
                0,
                true,
                JesterRuntime.PSYCHO_TICKS,
                Optional.of(UUID.randomUUID()),
                1.0D,
                2.0D,
                3.0D
        ));

        StrawWinEvents.WinContribution.Builder passengerBuilder = StrawWinEvents.WinContribution.builder();
        JesterRuntime.collectWinContribution(jester, state, StrawWinEvents.DefaultWin.PASSENGERS, passengerBuilder);
        assertTrue(passengerBuilder.build().suppressDefaultWin());

        StrawWinEvents.WinContribution.Builder killerBuilder = StrawWinEvents.WinContribution.builder();
        JesterRuntime.collectWinContribution(jester, state, StrawWinEvents.DefaultWin.KILLERS, killerBuilder);
        assertTrue(killerBuilder.build().suppressDefaultWin());

        StrawWinEvents.WinContribution.Builder timeBuilder = StrawWinEvents.WinContribution.builder();
        JesterRuntime.collectWinContribution(jester, state, StrawWinEvents.DefaultWin.TIME, timeBuilder);
        assertFalse(timeBuilder.build().suppressDefaultWin());
    }

    private static JesterWinPolicy.KillAttemptInput killAttempt(
            boolean victimJester,
            boolean jesterInStasis,
            boolean jesterInPsychoMode,
            Optional<UUID> killerUuid,
            boolean killerInnocent,
            Identifier deathReason
    ) {
        return new JesterWinPolicy.KillAttemptInput(
                victimJester,
                jesterInStasis,
                jesterInPsychoMode,
                killerUuid,
                killerInnocent,
                deathReason
        );
    }
}
