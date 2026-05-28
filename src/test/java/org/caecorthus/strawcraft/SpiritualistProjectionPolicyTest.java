package org.caecorthus.strawcraft;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritualistProjectionPolicyTest {
    @Test
    void liveSpiritualistCanStartAndReturnProjectionWhenCooldownReady() {
        assertEquals(SpiritualistProjectionPolicy.Result.START_PROJECTING,
                SpiritualistProjectionPolicy.validateToggle(validToggle(false)));
        assertEquals(SpiritualistProjectionPolicy.Result.RETURN_TO_BODY,
                SpiritualistProjectionPolicy.validateToggle(validToggle(true).withCooldownReady(false)));
        assertEquals(SpiritualistProjectionPolicy.Result.RETURN_TO_BODY,
                SpiritualistProjectionPolicy.validateToggle(validToggle(true).withPlayerSwallowed(true)));
    }

    @Test
    void forgedOrStaleProjectionIntentCannotBypassServerGuards() {
        assertBlocked(validToggle(false).withRoundRunning(false));
        assertBlocked(validToggle(false).withSpiritualistRole(false));
        assertBlocked(validToggle(false).withPlayerAlive(false));
        assertBlocked(validToggle(false).withPlayerSwallowed(true));
        assertBlocked(validToggle(false).withCooldownReady(false));
    }

    @Test
    void projectionStateAndForcedReturnCooldownAreRecordedInRoleState() {
        NoellesRoleState state = new NoellesRoleState();

        SpiritualistProjectionPolicy.startProjecting(state, 10.5D, 64.0D, -4.25D, 400L);

        NoellesRoleState.SpiritualistProjection projection = state.spiritualistProjection().orElseThrow();
        assertEquals(10.5D, projection.bodyX());
        assertEquals(64.0D, projection.bodyY());
        assertEquals(-4.25D, projection.bodyZ());
        assertEquals(400L, projection.startedAtTick());
        assertTrue(SpiritualistProjectionPolicy.isProjecting(state));

        SpiritualistProjectionPolicy.returnToBody(state, 440L);

        assertTrue(state.spiritualistProjection().isEmpty());
        assertFalse(SpiritualistProjectionPolicy.isProjecting(state));
        assertEquals(SpiritualistProjectionPolicy.FORCED_RETURN_COOLDOWN_TICKS,
                state.getRemainingAbilityCooldown(SpiritualistProjectionPolicy.ABILITY_ID, 440L));

        SpiritualistProjectionPolicy.startProjecting(state, 10.5D, 64.0D, -4.25D, 460L);
        SpiritualistProjectionPolicy.forceReturn(state, 460L);

        assertTrue(state.spiritualistProjection().isEmpty());
        assertFalse(SpiritualistProjectionPolicy.isProjecting(state));
        assertEquals(SpiritualistProjectionPolicy.FORCED_RETURN_COOLDOWN_TICKS,
                state.getRemainingAbilityCooldown(SpiritualistProjectionPolicy.ABILITY_ID, 460L));
    }

    @Test
    void returnToBodyApiRequiresCooldownTime() {
        assertTrue(Arrays.stream(SpiritualistProjectionPolicy.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("returnToBody"))
                .allMatch(SpiritualistProjectionPolicyTest::hasCurrentGameTimeParameter));
    }

    private static boolean hasCurrentGameTimeParameter(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(type -> type == long.class);
    }

    private static void assertBlocked(SpiritualistProjectionPolicy.Input input) {
        assertTrue(SpiritualistProjectionPolicy.validateToggle(input).blocked());
    }

    private static SpiritualistProjectionPolicy.Input validToggle(boolean projecting) {
        return new SpiritualistProjectionPolicy.Input(true, true, true, false, true, projecting);
    }
}
