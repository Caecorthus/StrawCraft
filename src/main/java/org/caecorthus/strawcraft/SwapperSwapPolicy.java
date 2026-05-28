package org.caecorthus.strawcraft;

import java.util.Objects;

public final class SwapperSwapPolicy {
    public static final String ABILITY_ID = "swapper_swap";
    public static final int SWAP_COOLDOWN_TICKS = 45 * 20;

    private SwapperSwapPolicy() {
    }

    public static ValidationResult validate(InteractionInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return ValidationResult.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.swapperRole()) {
            return ValidationResult.NOT_SWAPPER;
        }
        if (!input.swapperAlive()) {
            return ValidationResult.SWAPPER_NOT_ALIVE;
        }
        if (!input.targetAPresent() || !input.targetBPresent()
                || input.swapperIsTargetA() || input.swapperIsTargetB() || input.sameTarget()) {
            return ValidationResult.INVALID_TARGETS;
        }
        if (!input.targetAAssigned() || !input.targetBAssigned() || !input.targetAAlive() || !input.targetBAlive()) {
            return ValidationResult.TARGET_NOT_ACTIVE;
        }
        if (!input.sameWorld()) {
            return ValidationResult.WRONG_DIMENSION;
        }
        if (!input.cooldownReady()) {
            return ValidationResult.COOLDOWN;
        }
        if (!input.targetADestinationSafe() || !input.targetBDestinationSafe()) {
            return ValidationResult.UNSAFE_DESTINATION;
        }
        return ValidationResult.ALLOWED;
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_SWAPPER,
        SWAPPER_NOT_ALIVE,
        INVALID_TARGETS,
        TARGET_NOT_ACTIVE,
        WRONG_DIMENSION,
        COOLDOWN,
        UNSAFE_DESTINATION;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record InteractionInput(
            boolean roundRunning,
            boolean swapperRole,
            boolean swapperAlive,
            boolean targetAPresent,
            boolean targetBPresent,
            boolean swapperIsTargetA,
            boolean swapperIsTargetB,
            boolean sameTarget,
            boolean targetAAssigned,
            boolean targetBAssigned,
            boolean targetAAlive,
            boolean targetBAlive,
            boolean sameWorld,
            boolean cooldownReady,
            boolean targetADestinationSafe,
            boolean targetBDestinationSafe
    ) {
        public InteractionInput withRoundRunning(boolean value) {
            return new InteractionInput(value, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withSwapperRole(boolean value) {
            return new InteractionInput(roundRunning, value, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withSwapperAlive(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, value, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetAPresent(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, value, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetBPresent(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, value,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withSwapperIsTargetA(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    value, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withSwapperIsTargetB(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, value, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withSameTarget(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, value, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetAAssigned(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, value, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetBAssigned(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, value,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetAAlive(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    value, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetBAlive(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, value, sameWorld, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withSameWorld(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, value, cooldownReady, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withCooldownReady(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, value, targetADestinationSafe,
                    targetBDestinationSafe);
        }

        public InteractionInput withTargetADestinationSafe(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, value, targetBDestinationSafe);
        }

        public InteractionInput withTargetBDestinationSafe(boolean value) {
            return new InteractionInput(roundRunning, swapperRole, swapperAlive, targetAPresent, targetBPresent,
                    swapperIsTargetA, swapperIsTargetB, sameTarget, targetAAssigned, targetBAssigned,
                    targetAAlive, targetBAlive, sameWorld, cooldownReady, targetADestinationSafe, value);
        }
    }
}
