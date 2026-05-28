package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScavengerHiddenBodyTargetingTest {
    @Test
    void hiddenScavengerBodyBlocksTargetingEvenWhenVanillaWouldAllowIt() {
        assertFalse(ScavengerHiddenBodyTargeting.allowsTargeting(true, true));
    }

    @Test
    void visibleBodyPreservesVanillaTargetingDecision() {
        assertTrue(ScavengerHiddenBodyTargeting.allowsTargeting(false, true));
        assertFalse(ScavengerHiddenBodyTargeting.allowsTargeting(false, false));
    }
}
