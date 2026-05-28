package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Objects;

public final class SpiritualistProjectionPolicy {
    public static final Identifier SPIRITUALIST_ROLE = StrawCraft.id("spiritualist");
    public static final String ABILITY_ID = "spiritualist_projection";
    public static final int FORCED_RETURN_COOLDOWN_TICKS = 60 * 20;
    public static final double BODY_TELEPORT_THRESHOLD_SQUARED = 2.0D;

    private SpiritualistProjectionPolicy() {
    }

    public static Result validateToggle(Input input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return Result.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.spiritualistRole()) {
            return Result.NOT_SPIRITUALIST;
        }
        if (!input.playerAlive()) {
            return Result.PLAYER_NOT_ALIVE;
        }
        if (input.currentlyProjecting()) {
            return Result.RETURN_TO_BODY;
        }
        if (input.playerSwallowed()) {
            return Result.PLAYER_SWALLOWED;
        }
        if (!input.cooldownReady()) {
            return Result.COOLDOWN;
        }
        return Result.START_PROJECTING;
    }

    public static void startProjecting(
            NoellesRoleState state,
            double bodyX,
            double bodyY,
            double bodyZ,
            long currentGameTime
    ) {
        state.setSpiritualistProjection(new NoellesRoleState.SpiritualistProjection(
                bodyX,
                bodyY,
                bodyZ,
                currentGameTime
        ));
    }

    public static void startProjecting(
            NoellesRoleStateComponent state,
            double bodyX,
            double bodyY,
            double bodyZ,
            long currentGameTime
    ) {
        state.setSpiritualistProjection(new NoellesRoleState.SpiritualistProjection(
                bodyX,
                bodyY,
                bodyZ,
                currentGameTime
        ));
    }

    public static void returnToBody(NoellesRoleState state, long currentGameTime) {
        state.clearSpiritualistProjection();
        state.setAbilityCooldown(ABILITY_ID, currentGameTime, FORCED_RETURN_COOLDOWN_TICKS);
    }

    public static void returnToBody(NoellesRoleStateComponent state, long currentGameTime) {
        state.clearSpiritualistProjection();
        state.setAbilityCooldown(ABILITY_ID, currentGameTime, FORCED_RETURN_COOLDOWN_TICKS);
    }

    public static void forceReturn(NoellesRoleState state, long currentGameTime) {
        state.clearSpiritualistProjection();
        state.setAbilityCooldown(ABILITY_ID, currentGameTime, FORCED_RETURN_COOLDOWN_TICKS);
    }

    public static void forceReturn(NoellesRoleStateComponent state, long currentGameTime) {
        state.clearSpiritualistProjection();
        state.setAbilityCooldown(ABILITY_ID, currentGameTime, FORCED_RETURN_COOLDOWN_TICKS);
    }

    public static boolean isProjecting(NoellesRoleState state) {
        return state.spiritualistProjection().isPresent();
    }

    public static boolean isProjecting(NoellesRoleStateComponent state) {
        return state.spiritualistProjection().isPresent();
    }

    public static boolean movedTooFarFromBody(NoellesRoleState.SpiritualistProjection projection, double x, double y, double z) {
        double dx = x - projection.bodyX();
        double dy = y - projection.bodyY();
        double dz = z - projection.bodyZ();
        return dx * dx + dy * dy + dz * dz > BODY_TELEPORT_THRESHOLD_SQUARED;
    }

    public enum Result {
        START_PROJECTING,
        RETURN_TO_BODY,
        NOT_IN_ACTIVE_ROUND,
        NOT_SPIRITUALIST,
        PLAYER_NOT_ALIVE,
        PLAYER_SWALLOWED,
        COOLDOWN;

        public boolean blocked() {
            return this != START_PROJECTING && this != RETURN_TO_BODY;
        }
    }

    public record Input(
            boolean roundRunning,
            boolean spiritualistRole,
            boolean playerAlive,
            boolean playerSwallowed,
            boolean cooldownReady,
            boolean currentlyProjecting
    ) {
        public Input withRoundRunning(boolean value) {
            return new Input(value, spiritualistRole, playerAlive, playerSwallowed, cooldownReady, currentlyProjecting);
        }

        public Input withSpiritualistRole(boolean value) {
            return new Input(roundRunning, value, playerAlive, playerSwallowed, cooldownReady, currentlyProjecting);
        }

        public Input withPlayerAlive(boolean value) {
            return new Input(roundRunning, spiritualistRole, value, playerSwallowed, cooldownReady, currentlyProjecting);
        }

        public Input withPlayerSwallowed(boolean value) {
            return new Input(roundRunning, spiritualistRole, playerAlive, value, cooldownReady, currentlyProjecting);
        }

        public Input withCooldownReady(boolean value) {
            return new Input(roundRunning, spiritualistRole, playerAlive, playerSwallowed, value, currentlyProjecting);
        }

        public Input withCurrentlyProjecting(boolean value) {
            return new Input(roundRunning, spiritualistRole, playerAlive, playerSwallowed, cooldownReady, value);
        }
    }
}
