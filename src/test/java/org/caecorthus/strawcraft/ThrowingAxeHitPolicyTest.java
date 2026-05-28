package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowingAxeHitPolicyTest {
    @Test
    void ignoresOwnerDeadTargetsAndNonSurvivalTargetsWithoutRecordingAHit() {
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ThrowingAxeHitPolicy policy = new ThrowingAxeHitPolicy();

        assertEquals(ThrowingAxeHitPolicy.Result.IGNORE_OWNER, policy.evaluateHit(input(owner, owner, true, true)));
        assertEquals(ThrowingAxeHitPolicy.Result.IGNORE_INVALID_TARGET, policy.evaluateHit(input(owner, target, false, true)));
        assertEquals(ThrowingAxeHitPolicy.Result.IGNORE_INVALID_TARGET, policy.evaluateHit(input(owner, target, true, false)));
        assertFalse(policy.hasHit(target));
    }

    @Test
    void killsAliveSurvivalTargetAndRecordsTheTargetAsHit() {
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ThrowingAxeHitPolicy policy = new ThrowingAxeHitPolicy();

        ThrowingAxeHitPolicy.Result result = policy.evaluateHit(input(owner, target, true, true));

        assertEquals(ThrowingAxeHitPolicy.Result.KILL_TARGET, result);
        assertTrue(result.killsTarget());
        assertTrue(policy.hasHit(target));
        assertEquals(1, policy.hitCount());
    }

    @Test
    void ignoresRepeatHitsAfterFirstKillDecision() {
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ThrowingAxeHitPolicy policy = new ThrowingAxeHitPolicy();

        assertEquals(ThrowingAxeHitPolicy.Result.KILL_TARGET, policy.evaluateHit(input(owner, target, true, true)));
        assertEquals(ThrowingAxeHitPolicy.Result.IGNORE_REPEAT_HIT, policy.evaluateHit(input(owner, target, true, true)));
        assertEquals(1, policy.hitCount());
    }

    private static ThrowingAxeHitPolicy.Input input(UUID owner, UUID target, boolean targetAlive, boolean targetSurvival) {
        return new ThrowingAxeHitPolicy.Input(owner, target, targetAlive, targetSurvival);
    }
}
