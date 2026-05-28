package org.caecorthus.strawcraft;

import java.util.Objects;

public final class ReporterMarkPolicy {
    public static final String ABILITY_ID = "reporter_mark";
    public static final int MARK_COOLDOWN_TICKS = 30 * 20;
    public static final int TRACKER_INTERVAL_TICKS = 20;
    public static final double MARK_RANGE_SQUARED = 9.0D;

    private ReporterMarkPolicy() {
    }

    public static ValidationResult validate(InteractionInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return ValidationResult.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.reporterRole()) {
            return ValidationResult.NOT_REPORTER;
        }
        if (!input.reporterAlive()) {
            return ValidationResult.REPORTER_NOT_ALIVE;
        }
        if (!input.targetPresent() || input.selfTarget()) {
            return ValidationResult.INVALID_TARGET;
        }
        if (!input.targetAssigned() || !input.targetAlive()) {
            return ValidationResult.TARGET_NOT_ACTIVE;
        }
        if (!input.sameWorld()) {
            return ValidationResult.WRONG_DIMENSION;
        }
        if (input.squaredDistance() > MARK_RANGE_SQUARED || !input.canSee()) {
            return ValidationResult.TARGET_OUT_OF_REACH;
        }
        if (!input.cooldownReady()) {
            return ValidationResult.COOLDOWN;
        }
        return ValidationResult.ALLOWED;
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_REPORTER,
        REPORTER_NOT_ALIVE,
        INVALID_TARGET,
        TARGET_NOT_ACTIVE,
        WRONG_DIMENSION,
        TARGET_OUT_OF_REACH,
        COOLDOWN;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record InteractionInput(
            boolean roundRunning,
            boolean reporterRole,
            boolean reporterAlive,
            boolean targetPresent,
            boolean selfTarget,
            boolean targetAssigned,
            boolean targetAlive,
            boolean sameWorld,
            double squaredDistance,
            boolean canSee,
            boolean cooldownReady
    ) {
        public InteractionInput withRoundRunning(boolean value) {
            return new InteractionInput(value, reporterRole, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withReporterRole(boolean value) {
            return new InteractionInput(roundRunning, value, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withReporterAlive(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, value, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetPresent(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, value, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSelfTarget(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, value,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetAssigned(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, selfTarget,
                    value, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetAlive(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, value, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSameWorld(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, value, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSquaredDistance(double value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, value, canSee, cooldownReady);
        }

        public InteractionInput withCanSee(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, value, cooldownReady);
        }

        public InteractionInput withCooldownReady(boolean value) {
            return new InteractionInput(roundRunning, reporterRole, reporterAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, value);
        }
    }
}
