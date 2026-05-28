package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwapperSwapPolicyTest {
    @Test
    void allowsRunningLiveSwapperToSwapTwoLiveAssignedSameWorldSafeTargetsWhenCooldownReady() {
        assertEquals(SwapperSwapPolicy.ValidationResult.ALLOWED,
                SwapperSwapPolicy.validate(validInteraction()));
    }

    @Test
    void rejectsForgedOrStaleClientTargetAndActorStates() {
        assertBlocked(validInteraction().withRoundRunning(false));
        assertBlocked(validInteraction().withSwapperRole(false));
        assertBlocked(validInteraction().withSwapperAlive(false));
        assertBlocked(validInteraction().withTargetAPresent(false));
        assertBlocked(validInteraction().withTargetBPresent(false));
        assertBlocked(validInteraction().withSwapperIsTargetA(true));
        assertBlocked(validInteraction().withSwapperIsTargetB(true));
        assertBlocked(validInteraction().withSameTarget(true));
        assertBlocked(validInteraction().withTargetAAssigned(false));
        assertBlocked(validInteraction().withTargetBAssigned(false));
        assertBlocked(validInteraction().withTargetAAlive(false));
        assertBlocked(validInteraction().withTargetBAlive(false));
        assertBlocked(validInteraction().withSameWorld(false));
        assertBlocked(validInteraction().withCooldownReady(false));
        assertBlocked(validInteraction().withTargetADestinationSafe(false));
        assertBlocked(validInteraction().withTargetBDestinationSafe(false));
    }

    private static void assertBlocked(SwapperSwapPolicy.InteractionInput input) {
        assertTrue(SwapperSwapPolicy.validate(input).blocked());
    }

    private static SwapperSwapPolicy.InteractionInput validInteraction() {
        return new SwapperSwapPolicy.InteractionInput(
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );
    }
}
