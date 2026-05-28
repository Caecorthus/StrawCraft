package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowingAxePowerPolicyTest {
    @Test
    void rejectsShortChargesBelowMinimumThrowPower() {
        ThrowingAxePowerPolicy.Decision decision = ThrowingAxePowerPolicy.evaluateChargeTicks(2);

        assertFalse(decision.accepted());
        assertTrue(decision.power() < ThrowingAxePowerPolicy.MIN_THROW_POWER);
    }

    @Test
    void acceptsChargedThrowsAtOrAboveMinimumPower() {
        ThrowingAxePowerPolicy.Decision decision = ThrowingAxePowerPolicy.evaluateChargeTicks(7);

        assertTrue(decision.accepted());
        assertTrue(decision.power() >= ThrowingAxePowerPolicy.MIN_THROW_POWER);
        assertEquals(0.27416667F, decision.power(), 0.00001F);
    }

    @Test
    void usesCappedVanillaStyleChargeCurveForFutureProjectileVelocity() {
        assertEquals(0.0F, ThrowingAxePowerPolicy.evaluateChargeTicks(-20).power());
        assertEquals(0.41666666F, ThrowingAxePowerPolicy.evaluateChargeTicks(10).power(), 0.00001F);
        assertEquals(1.0F, ThrowingAxePowerPolicy.evaluateChargeTicks(20).power());
        assertEquals(1.0F, ThrowingAxePowerPolicy.evaluateChargeTicks(200).power());
    }
}
