package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporterMarkPolicyTest {
    @Test
    void allowsRunningLiveReporterToMarkLiveAssignedVisibleNearbyTargetWhenCooldownReady() {
        assertEquals(ReporterMarkPolicy.ValidationResult.ALLOWED,
                ReporterMarkPolicy.validate(validInteraction()));
    }

    @Test
    void rejectsForgedOrStaleClientTargetAndActorStates() {
        assertBlocked(validInteraction().withRoundRunning(false));
        assertBlocked(validInteraction().withReporterRole(false));
        assertBlocked(validInteraction().withReporterAlive(false));
        assertBlocked(validInteraction().withTargetPresent(false));
        assertBlocked(validInteraction().withSelfTarget(true));
        assertBlocked(validInteraction().withTargetAssigned(false));
        assertBlocked(validInteraction().withTargetAlive(false));
        assertBlocked(validInteraction().withSameWorld(false));
        assertBlocked(validInteraction().withSquaredDistance(ReporterMarkPolicy.MARK_RANGE_SQUARED + 0.01D));
        assertBlocked(validInteraction().withCanSee(false));
        assertBlocked(validInteraction().withCooldownReady(false));
    }

    private static void assertBlocked(ReporterMarkPolicy.InteractionInput input) {
        assertTrue(ReporterMarkPolicy.validate(input).blocked());
    }

    private static ReporterMarkPolicy.InteractionInput validInteraction() {
        return new ReporterMarkPolicy.InteractionInput(
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                ReporterMarkPolicy.MARK_RANGE_SQUARED,
                true,
                true
        );
    }
}
