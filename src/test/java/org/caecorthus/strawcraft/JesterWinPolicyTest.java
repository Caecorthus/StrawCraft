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
