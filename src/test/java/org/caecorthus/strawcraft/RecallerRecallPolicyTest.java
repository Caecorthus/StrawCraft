package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecallerRecallPolicyTest {
    @Test
    void firstValidActivationStoresRecallPoint() {
        assertEquals(RecallerRecallPolicy.Result.STORE_POINT, validate(true, true, true, true, false, true, 0));
    }

    @Test
    void secondValidActivationRecallsOnlyWithSameDimensionAndEnoughBalance() {
        assertEquals(RecallerRecallPolicy.Result.RECALL, validate(true, true, true, true, true, true, 100));
        assertEquals(RecallerRecallPolicy.Result.INSUFFICIENT_BALANCE, validate(true, true, true, true, true, true, 99));
        assertEquals(RecallerRecallPolicy.Result.WRONG_DIMENSION, validate(true, true, true, true, true, false, 100));
    }

    @Test
    void forgedOrStaleActivationCannotBypassServerGuards() {
        assertEquals(RecallerRecallPolicy.Result.NOT_RUNNING, validate(false, true, true, true, true, true, 100));
        assertEquals(RecallerRecallPolicy.Result.NOT_RECALLER, validate(true, false, true, true, true, true, 100));
        assertEquals(RecallerRecallPolicy.Result.NOT_ALIVE, validate(true, true, false, true, true, true, 100));
        assertEquals(RecallerRecallPolicy.Result.COOLDOWN, validate(true, true, true, false, true, true, 100));
    }

    private static RecallerRecallPolicy.Result validate(
            boolean roundRunning,
            boolean recallerRole,
            boolean playerAlive,
            boolean cooldownReady,
            boolean hasRecallPoint,
            boolean sameDimension,
            int balance
    ) {
        return RecallerRecallPolicy.validateActivation(new RecallerRecallPolicy.Input(
                roundRunning,
                recallerRole,
                playerAlive,
                cooldownReady,
                hasRecallPoint,
                sameDimension,
                balance
        ));
    }
}
