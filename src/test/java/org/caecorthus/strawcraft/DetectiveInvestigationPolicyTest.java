package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectiveInvestigationPolicyTest {
    @Test
    void targetWithRecentNonImmuneKillIsSuspicious() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID target = UUID.randomUUID();

        history.recordKill(target, UUID.randomUUID(), GameConstants.DeathReasons.KNIFE, 100L);

        assertEquals(DetectiveInvestigationPolicy.Result.SUSPICIOUS,
                DetectiveInvestigationPolicy.investigate(target, history, 101L));
    }

    @Test
    void targetWithoutRecentNonImmuneKillIsClear() {
        DetectiveKillHistory history = new DetectiveKillHistory();
        UUID target = UUID.randomUUID();

        history.recordKill(target, UUID.randomUUID(), GameConstants.DeathReasons.POISON, 100L);

        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveInvestigationPolicy.investigate(target, history, 101L));
        assertEquals(DetectiveInvestigationPolicy.Result.CLEAR,
                DetectiveInvestigationPolicy.investigate(null, history, 101L));
    }

    @Test
    void interactionValidationRequiresLiveDetectiveAndLiveVisibleNearbyAssignedTarget() {
        assertEquals(DetectiveInvestigationPolicy.ValidationResult.ALLOWED,
                DetectiveInvestigationPolicy.validateInteraction(validInteraction()));

        assertBlocked(validInteraction().withRoundRunning(false));
        assertBlocked(validInteraction().withDetectiveRole(false));
        assertBlocked(validInteraction().withDetectiveAlive(false));
        assertBlocked(validInteraction().withTargetPresent(false));
        assertBlocked(validInteraction().withSelfTarget(true));
        assertBlocked(validInteraction().withTargetAssigned(false));
        assertBlocked(validInteraction().withTargetAlive(false));
        assertBlocked(validInteraction().withSquaredDistance(9.01));
        assertBlocked(validInteraction().withCanSee(false));
        assertBlocked(validInteraction().withCooldownReady(false));
    }

    private static void assertBlocked(DetectiveInvestigationPolicy.InteractionInput input) {
        assertTrue(DetectiveInvestigationPolicy.validateInteraction(input).blocked());
    }

    private static DetectiveInvestigationPolicy.InteractionInput validInteraction() {
        return new DetectiveInvestigationPolicy.InteractionInput(
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                9.0,
                true,
                true
        );
    }
}
