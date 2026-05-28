package org.caecorthus.strawcraft;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class DetectiveInvestigationPolicy {
    private DetectiveInvestigationPolicy() {
    }

    public static Result investigate(@Nullable UUID targetUuid, DetectiveKillHistory history, long currentGameTime) {
        Objects.requireNonNull(history, "history");
        if (history.hasRecentNonImmuneKill(targetUuid, currentGameTime)) {
            return Result.SUSPICIOUS;
        }
        return Result.CLEAR;
    }

    public static ValidationResult validateInteraction(InteractionInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return ValidationResult.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.detectiveRole()) {
            return ValidationResult.NOT_DETECTIVE;
        }
        if (!input.detectiveAlive()) {
            return ValidationResult.DETECTIVE_NOT_ALIVE;
        }
        if (!input.targetPresent() || input.selfTarget()) {
            return ValidationResult.INVALID_TARGET;
        }
        if (!input.targetAssigned() || !input.targetAlive()) {
            return ValidationResult.TARGET_NOT_ACTIVE;
        }
        if (input.squaredDistance() > 9.0 || !input.canSee()) {
            return ValidationResult.TARGET_OUT_OF_REACH;
        }
        if (!input.cooldownReady()) {
            return ValidationResult.COOLDOWN;
        }
        return ValidationResult.ALLOWED;
    }

    public enum Result {
        SUSPICIOUS,
        CLEAR
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_DETECTIVE,
        DETECTIVE_NOT_ALIVE,
        INVALID_TARGET,
        TARGET_NOT_ACTIVE,
        TARGET_OUT_OF_REACH,
        COOLDOWN;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record InteractionInput(
            boolean roundRunning,
            boolean detectiveRole,
            boolean detectiveAlive,
            boolean targetPresent,
            boolean selfTarget,
            boolean targetAssigned,
            boolean targetAlive,
            double squaredDistance,
            boolean canSee,
            boolean cooldownReady
    ) {
        public InteractionInput withRoundRunning(boolean value) {
            return new InteractionInput(value, detectiveRole, detectiveAlive, targetPresent, selfTarget, targetAssigned,
                    targetAlive, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withDetectiveRole(boolean value) {
            return new InteractionInput(roundRunning, value, detectiveAlive, targetPresent, selfTarget, targetAssigned,
                    targetAlive, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withDetectiveAlive(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, value, targetPresent, selfTarget, targetAssigned,
                    targetAlive, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetPresent(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, value, selfTarget, targetAssigned,
                    targetAlive, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSelfTarget(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, targetPresent, value, targetAssigned,
                    targetAlive, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetAssigned(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, targetPresent, selfTarget, value,
                    targetAlive, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetAlive(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, targetPresent, selfTarget,
                    targetAssigned, value, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSquaredDistance(double value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, value, canSee, cooldownReady);
        }

        public InteractionInput withCanSee(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, squaredDistance, value, cooldownReady);
        }

        public InteractionInput withCooldownReady(boolean value) {
            return new InteractionInput(roundRunning, detectiveRole, detectiveAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, squaredDistance, canSee, value);
        }
    }
}
