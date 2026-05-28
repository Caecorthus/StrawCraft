package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PathogenInfectionPolicy {
    public static final Identifier PATHOGEN_ROLE = StrawCraft.id("pathogen");
    public static final String ABILITY_ID = "pathogen_infection";
    public static final double INFECTION_RANGE_SQUARED = 3.0D * 3.0D;

    private static final String BASE_COOLDOWN_COUNTER = "pathogen.base_cooldown_ticks";

    private PathogenInfectionPolicy() {
    }

    public static int baseCooldownTicks(int startingPlayerCount) {
        if (startingPlayerCount > 24) {
            return 7 * 20;
        }
        if (startingPlayerCount >= 18) {
            return 10 * 20;
        }
        if (startingPlayerCount >= 12) {
            return 15 * 20;
        }
        return 20 * 20;
    }

    public static void resetParticipantState(NoellesRoleState state) {
        state.clearPathogenInfection();
        state.clearNeutralWinClaim(PATHOGEN_ROLE);
    }

    public static void resetParticipantState(NoellesRoleStateComponent state) {
        state.clearPathogenInfection();
        state.clearNeutralWinClaim(PATHOGEN_ROLE);
    }

    public static void resetPathogenState(NoellesRoleState state, int startingPlayerCount) {
        resetParticipantState(state);
        state.setCounter(BASE_COOLDOWN_COUNTER, baseCooldownTicks(startingPlayerCount));
        state.clearAbilityCooldown(ABILITY_ID);
    }

    public static void resetPathogenState(NoellesRoleStateComponent state, int startingPlayerCount) {
        resetParticipantState(state);
        state.setCounter(BASE_COOLDOWN_COUNTER, baseCooldownTicks(startingPlayerCount));
        state.clearAbilityCooldown(ABILITY_ID);
    }

    public static int baseCooldownTicks(NoellesRoleState state, int fallbackPlayerCount) {
        int stored = state.getCounter(BASE_COOLDOWN_COUNTER);
        return stored > 0 ? stored : baseCooldownTicks(fallbackPlayerCount);
    }

    public static int baseCooldownTicks(NoellesRoleStateComponent state, int fallbackPlayerCount) {
        int stored = state.getCounter(BASE_COOLDOWN_COUNTER);
        return stored > 0 ? stored : baseCooldownTicks(fallbackPlayerCount);
    }

    public static InfectionAttempt evaluate(ActivationInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return InfectionAttempt.blocked(ValidationResult.NOT_IN_ACTIVE_ROUND);
        }
        if (!input.pathogenRole()) {
            return InfectionAttempt.blocked(ValidationResult.NOT_PATHOGEN);
        }
        if (!input.pathogenAlive()) {
            return InfectionAttempt.blocked(ValidationResult.PATHOGEN_NOT_ALIVE);
        }
        if (!input.cooldownReady()) {
            return InfectionAttempt.blocked(ValidationResult.COOLDOWN);
        }

        Optional<UUID> target = input.candidates().stream()
                .filter(candidate -> isValidTarget(input.pathogenUuid(), candidate))
                .min(Comparator
                        .comparingDouble(TargetCandidate::squaredDistance)
                        .thenComparing(candidate -> candidate.uuid().toString()))
                .map(TargetCandidate::uuid);
        return target
                .map(InfectionAttempt::allowed)
                .orElseGet(() -> InfectionAttempt.blocked(ValidationResult.NO_TARGET));
    }

    private static boolean isValidTarget(UUID pathogenUuid, TargetCandidate candidate) {
        return !pathogenUuid.equals(candidate.uuid())
                && candidate.assigned()
                && candidate.alive()
                && !candidate.infected()
                && candidate.visible()
                && candidate.squaredDistance() <= INFECTION_RANGE_SQUARED;
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_PATHOGEN,
        PATHOGEN_NOT_ALIVE,
        COOLDOWN,
        NO_TARGET;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record InfectionAttempt(ValidationResult result, Optional<UUID> targetUuid) {
        public InfectionAttempt {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(targetUuid, "targetUuid");
        }

        private static InfectionAttempt allowed(UUID targetUuid) {
            return new InfectionAttempt(ValidationResult.ALLOWED, Optional.of(targetUuid));
        }

        private static InfectionAttempt blocked(ValidationResult result) {
            return new InfectionAttempt(result, Optional.empty());
        }
    }

    public record ActivationInput(
            boolean roundRunning,
            boolean pathogenRole,
            boolean pathogenAlive,
            boolean cooldownReady,
            UUID pathogenUuid,
            List<TargetCandidate> candidates
    ) {
        public ActivationInput {
            Objects.requireNonNull(pathogenUuid, "pathogenUuid");
            candidates = List.copyOf(candidates);
        }

        public ActivationInput withRoundRunning(boolean value) {
            return new ActivationInput(value, pathogenRole, pathogenAlive, cooldownReady, pathogenUuid, candidates);
        }

        public ActivationInput withPathogenRole(boolean value) {
            return new ActivationInput(roundRunning, value, pathogenAlive, cooldownReady, pathogenUuid, candidates);
        }

        public ActivationInput withPathogenAlive(boolean value) {
            return new ActivationInput(roundRunning, pathogenRole, value, cooldownReady, pathogenUuid, candidates);
        }

        public ActivationInput withCooldownReady(boolean value) {
            return new ActivationInput(roundRunning, pathogenRole, pathogenAlive, value, pathogenUuid, candidates);
        }

        public ActivationInput withCandidates(List<TargetCandidate> value) {
            return new ActivationInput(roundRunning, pathogenRole, pathogenAlive, cooldownReady, pathogenUuid, value);
        }
    }

    public record TargetCandidate(
            UUID uuid,
            boolean assigned,
            boolean alive,
            boolean infected,
            boolean visible,
            double squaredDistance
    ) {
        public TargetCandidate {
            Objects.requireNonNull(uuid, "uuid");
        }

        public TargetCandidate withAssigned(boolean value) {
            return new TargetCandidate(uuid, value, alive, infected, visible, squaredDistance);
        }

        public TargetCandidate withAlive(boolean value) {
            return new TargetCandidate(uuid, assigned, value, infected, visible, squaredDistance);
        }

        public TargetCandidate withInfected(boolean value) {
            return new TargetCandidate(uuid, assigned, alive, value, visible, squaredDistance);
        }

        public TargetCandidate withVisible(boolean value) {
            return new TargetCandidate(uuid, assigned, alive, infected, value, squaredDistance);
        }
    }
}
