package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoodooBondPolicyTest {
    @Test
    void allowsRunningLiveVoodooToBondLiveAssignedVisibleNearbyTargetWhenCooldownReady() {
        assertEquals(VoodooBondPolicy.ValidationResult.ALLOWED,
                VoodooBondPolicy.validate(validInteraction()));
    }

    @Test
    void rejectsForgedOrStaleClientTargetAndActorStates() {
        assertBlocked(validInteraction().withRoundRunning(false));
        assertBlocked(validInteraction().withVoodooRole(false));
        assertBlocked(validInteraction().withVoodooAlive(false));
        assertBlocked(validInteraction().withTargetPresent(false));
        assertBlocked(validInteraction().withSelfTarget(true));
        assertBlocked(validInteraction().withTargetAssigned(false));
        assertBlocked(validInteraction().withTargetAlive(false));
        assertBlocked(validInteraction().withSameWorld(false));
        assertBlocked(validInteraction().withSquaredDistance(VoodooBondPolicy.BOND_RANGE_SQUARED + 0.01D));
        assertBlocked(validInteraction().withCanSee(false));
        assertBlocked(validInteraction().withCooldownReady(false));
    }

    private static void assertBlocked(VoodooBondPolicy.InteractionInput input) {
        assertTrue(VoodooBondPolicy.validate(input).blocked());
    }

    private static VoodooBondPolicy.InteractionInput validInteraction() {
        return new VoodooBondPolicy.InteractionInput(
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                VoodooBondPolicy.BOND_RANGE_SQUARED,
                true,
                true
        );
    }
}
