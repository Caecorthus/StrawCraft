package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

public final class MorphlingDisguisePolicy {
    public static final Identifier MORPHLING_ROLE = StrawCraft.id("morphling");
    public static final String ABILITY_ID = "morphling_disguise";
    public static final int ACTIVE_TICKS = 35 * 20;
    public static final int RECOVERY_TICKS = 20 * 20;

    private MorphlingDisguisePolicy() {
    }

    public static ValidationResult validateStart(StartInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return ValidationResult.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.morphlingRole()) {
            return ValidationResult.NOT_MORPHLING;
        }
        if (!input.morphlingAlive() || input.morphlingSwallowed()) {
            return ValidationResult.MORPHLING_NOT_ACTIVE;
        }
        if (!input.targetPresent() || input.selfTarget()) {
            return ValidationResult.INVALID_TARGET;
        }
        if (!input.targetAssigned() || !input.targetAlive() || input.targetSwallowed() || !input.sameWorld()) {
            return ValidationResult.TARGET_NOT_ACTIVE;
        }
        if (!input.cooldownReady() || !input.recoveryReady()) {
            return ValidationResult.COOLDOWN;
        }
        return ValidationResult.ALLOWED;
    }

    public static NoellesRoleState.MorphlingDisguiseState startMorph(
            NoellesRoleState.MorphlingDisguiseState current,
            UUID targetUuid,
            long now
    ) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(targetUuid, "targetUuid");
        return new NoellesRoleState.MorphlingDisguiseState(
                java.util.Optional.of(targetUuid),
                ACTIVE_TICKS,
                now + ACTIVE_TICKS,
                current.corpseMode()
        );
    }

    public static NoellesRoleState.MorphlingDisguiseState stopMorph(NoellesRoleState.MorphlingDisguiseState current) {
        Objects.requireNonNull(current, "current");
        return new NoellesRoleState.MorphlingDisguiseState(
                java.util.Optional.empty(),
                -RECOVERY_TICKS,
                0L,
                current.corpseMode()
        );
    }

    public static NoellesRoleState.MorphlingDisguiseState toggleCorpseMode(
            NoellesRoleState.MorphlingDisguiseState current
    ) {
        Objects.requireNonNull(current, "current");
        return new NoellesRoleState.MorphlingDisguiseState(
                current.disguiseUuid(),
                current.morphTicks(),
                current.activeDeadlineTick(),
                !current.corpseMode()
        );
    }

    public static NoellesRoleState.MorphlingDisguiseState tick(
            NoellesRoleState.MorphlingDisguiseState current,
            boolean disguiseTargetStillValid
    ) {
        Objects.requireNonNull(current, "current");
        if (current.morphTicks() > 0) {
            if (!disguiseTargetStillValid || current.morphTicks() == 1) {
                return stopMorph(current);
            }
            return new NoellesRoleState.MorphlingDisguiseState(
                    current.disguiseUuid(),
                    current.morphTicks() - 1,
                    current.activeDeadlineTick(),
                    current.corpseMode()
            );
        }
        if (current.morphTicks() < 0) {
            int nextTicks = current.morphTicks() + 1;
            if (nextTicks == 0) {
                return new NoellesRoleState.MorphlingDisguiseState(
                        java.util.Optional.empty(),
                        0,
                        0L,
                        current.corpseMode()
                );
            }
            return new NoellesRoleState.MorphlingDisguiseState(
                    java.util.Optional.empty(),
                    nextTicks,
                    0L,
                    current.corpseMode()
            );
        }
        return current;
    }

    public static boolean isActive(NoellesRoleState.MorphlingDisguiseState current) {
        Objects.requireNonNull(current, "current");
        return current.morphTicks() > 0 && current.disguiseUuid().isPresent();
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_MORPHLING,
        MORPHLING_NOT_ACTIVE,
        INVALID_TARGET,
        TARGET_NOT_ACTIVE,
        COOLDOWN;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record StartInput(
            boolean roundRunning,
            boolean morphlingRole,
            boolean morphlingAlive,
            boolean morphlingSwallowed,
            boolean targetPresent,
            boolean selfTarget,
            boolean targetAssigned,
            boolean targetAlive,
            boolean targetSwallowed,
            boolean sameWorld,
            boolean cooldownReady,
            boolean recoveryReady
    ) {
        public StartInput withRoundRunning(boolean value) {
            return new StartInput(value, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent, selfTarget,
                    targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withMorphlingRole(boolean value) {
            return new StartInput(roundRunning, value, morphlingAlive, morphlingSwallowed, targetPresent, selfTarget,
                    targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withMorphlingAlive(boolean value) {
            return new StartInput(roundRunning, morphlingRole, value, morphlingSwallowed, targetPresent, selfTarget,
                    targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withMorphlingSwallowed(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, value, targetPresent, selfTarget,
                    targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withTargetPresent(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, value, selfTarget,
                    targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withSelfTarget(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent, value,
                    targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withTargetAssigned(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent,
                    selfTarget, value, targetAlive, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withTargetAlive(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent,
                    selfTarget, targetAssigned, value, targetSwallowed, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withTargetSwallowed(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent,
                    selfTarget, targetAssigned, targetAlive, value, sameWorld, cooldownReady, recoveryReady);
        }

        public StartInput withSameWorld(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent,
                    selfTarget, targetAssigned, targetAlive, targetSwallowed, value, cooldownReady, recoveryReady);
        }

        public StartInput withCooldownReady(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent,
                    selfTarget, targetAssigned, targetAlive, targetSwallowed, sameWorld, value, recoveryReady);
        }

        public StartInput withRecoveryReady(boolean value) {
            return new StartInput(roundRunning, morphlingRole, morphlingAlive, morphlingSwallowed, targetPresent,
                    selfTarget, targetAssigned, targetAlive, targetSwallowed, sameWorld, cooldownReady, value);
        }
    }
}
