package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectiveKillHistoryTest {
    @Test
    void recentNonImmuneKillMakesTargetSuspicious() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();

        history.recordKill(killer, victim, GameConstants.DeathReasons.KNIFE, 100L);

        assertTrue(history.hasRecentNonImmuneKill(killer, 100L + DetectiveKillHistory.DEFAULT_LOOKBACK_TICKS));
    }

    @Test
    void immuneReasonsStayClearForDetectiveInvestigation() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID poisoner = UUID.randomUUID();
        UUID bomber = UUID.randomUUID();
        UUID assassin = UUID.randomUUID();

        history.recordKill(poisoner, UUID.randomUUID(), GameConstants.DeathReasons.POISON, 100L);
        history.recordKill(bomber, UUID.randomUUID(), StrawDeathReasons.BOMB, 100L);
        history.recordKill(assassin, UUID.randomUUID(), StrawDeathReasons.ASSASSINATED, 100L);

        assertFalse(history.hasRecentNonImmuneKill(poisoner, 101L));
        assertFalse(history.hasRecentNonImmuneKill(bomber, 101L));
        assertFalse(history.hasRecentNonImmuneKill(assassin, 101L));
    }

    @Test
    void expiredKillIsClearAndCanBePruned() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID killer = UUID.randomUUID();

        history.recordKill(killer, UUID.randomUUID(), GameConstants.DeathReasons.GUN, 100L);
        history.expireOldKills(100L + DetectiveKillHistory.DEFAULT_LOOKBACK_TICKS + 1L);

        assertFalse(history.hasRecentNonImmuneKill(killer, 100L + DetectiveKillHistory.DEFAULT_LOOKBACK_TICKS + 1L));
    }

    @Test
    void resetClearsKillHistory() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID killer = UUID.randomUUID();

        history.recordKill(killer, UUID.randomUUID(), GameConstants.DeathReasons.BAT, 100L);
        history.reset();

        assertFalse(history.hasRecentNonImmuneKill(killer, 101L));
    }

    @Test
    void nullAndUnknownInputsAreSafeAndClear() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID killer = UUID.randomUUID();

        history.recordKill(null, UUID.randomUUID(), GameConstants.DeathReasons.KNIFE, 100L);
        history.recordKill(killer, UUID.randomUUID(), null, 100L);

        assertFalse(history.hasRecentNonImmuneKill(null, 101L));
        assertFalse(history.hasRecentNonImmuneKill(killer, 101L));
        assertFalse(history.hasRecentNonImmuneKill(UUID.randomUUID(), 101L));
        assertFalse(DetectiveKillHistory.isImmuneDeathReason(null));
        assertFalse(DetectiveKillHistory.isImmuneDeathReason(Identifier.of("strawcraft", "unknown")));
    }
}
