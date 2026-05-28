package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhantomInvisibilityPolicyTest {
    @Test
    void allowsRunningLivePhantomToBecomeInvisibleWhenCooldownReady() {
        assertEquals(PhantomInvisibilityPolicy.Result.ALLOWED,
                PhantomInvisibilityPolicy.validate(validActivation()));
    }

    @Test
    void forgedOrStaleActivationCannotBypassServerGuards() {
        assertBlocked(validActivation().withRoundRunning(false));
        assertBlocked(validActivation().withPhantomRole(false));
        assertBlocked(validActivation().withPhantomAlive(false));
        assertBlocked(validActivation().withCooldownReady(false));
    }

    private static void assertBlocked(PhantomInvisibilityPolicy.Input input) {
        assertTrue(PhantomInvisibilityPolicy.validate(input).blocked());
    }

    private static PhantomInvisibilityPolicy.Input validActivation() {
        return new PhantomInvisibilityPolicy.Input(true, true, true, true);
    }
}
