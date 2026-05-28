package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Objects;

public final class VoodooBondPolicy {
    public static final Identifier VOODOO_ROLE = StrawCraft.id("voodoo");
    public static final String ABILITY_ID = "voodoo_bond";
    public static final int BOND_COOLDOWN_TICKS = 30 * 20;
    public static final double BOND_RANGE_SQUARED = 9.0D;

    private VoodooBondPolicy() {
    }

    public static ValidationResult validate(InteractionInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return ValidationResult.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.voodooRole()) {
            return ValidationResult.NOT_VOODOO;
        }
        if (!input.voodooAlive()) {
            return ValidationResult.VOODOO_NOT_ALIVE;
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
        if (input.squaredDistance() > BOND_RANGE_SQUARED || !input.canSee()) {
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
        NOT_VOODOO,
        VOODOO_NOT_ALIVE,
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
            boolean voodooRole,
            boolean voodooAlive,
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
            return new InteractionInput(value, voodooRole, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withVoodooRole(boolean value) {
            return new InteractionInput(roundRunning, value, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withVoodooAlive(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, value, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetPresent(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, value, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSelfTarget(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, value,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetAssigned(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, selfTarget,
                    value, targetAlive, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withTargetAlive(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, value, sameWorld, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSameWorld(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, value, squaredDistance, canSee, cooldownReady);
        }

        public InteractionInput withSquaredDistance(double value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, value, canSee, cooldownReady);
        }

        public InteractionInput withCanSee(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, value, cooldownReady);
        }

        public InteractionInput withCooldownReady(boolean value) {
            return new InteractionInput(roundRunning, voodooRole, voodooAlive, targetPresent, selfTarget,
                    targetAssigned, targetAlive, sameWorld, squaredDistance, canSee, value);
        }
    }
}
