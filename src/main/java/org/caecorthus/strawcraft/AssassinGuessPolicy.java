package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AssassinGuessPolicy {
    public static final Identifier ASSASSIN_ROLE = StrawCraft.id("assassin");
    public static final String ABILITY_ID = "assassin_guess";
    public static final int COOLDOWN_TICKS = 60 * 20;

    private static final String GUESSES_REMAINING_COUNTER = "assassin.guesses_remaining";
    private static final String MAX_GUESSES_COUNTER = "assassin.max_guesses";

    private AssassinGuessPolicy() {
    }

    public static void resetRoundState(NoellesRoleState state, int totalPlayers, long now) {
        int maxGuesses = Math.max(0, totalPlayers);
        state.setCounter(MAX_GUESSES_COUNTER, maxGuesses);
        state.setCounter(GUESSES_REMAINING_COUNTER, maxGuesses);
        state.setAbilityCooldown(ABILITY_ID, now, COOLDOWN_TICKS);
    }

    public static void resetRoundState(NoellesRoleStateComponent state, int totalPlayers, long now) {
        int maxGuesses = Math.max(0, totalPlayers);
        state.setCounter(MAX_GUESSES_COUNTER, maxGuesses);
        state.setCounter(GUESSES_REMAINING_COUNTER, maxGuesses);
        state.tryBeginAbilityCooldown(ABILITY_ID, now, COOLDOWN_TICKS);
    }

    public static boolean canGuess(NoellesRoleState state, long now) {
        return guessesRemaining(state) > 0 && !state.isAbilityOnCooldown(ABILITY_ID, now);
    }

    public static boolean canGuess(NoellesRoleStateComponent state, long now) {
        return guessesRemaining(state) > 0 && !state.isAbilityOnCooldown(ABILITY_ID, now);
    }

    public static void useGuess(NoellesRoleState state, long now) {
        state.setCounter(GUESSES_REMAINING_COUNTER, Math.max(0, guessesRemaining(state) - 1));
        state.setAbilityCooldown(ABILITY_ID, now, COOLDOWN_TICKS);
    }

    public static void useGuess(NoellesRoleStateComponent state, long now) {
        state.setCounter(GUESSES_REMAINING_COUNTER, Math.max(0, guessesRemaining(state) - 1));
        state.clearAbilityCooldown(ABILITY_ID);
        state.tryBeginAbilityCooldown(ABILITY_ID, now, COOLDOWN_TICKS);
    }

    public static int guessesRemaining(NoellesRoleState state) {
        return state.getCounter(GUESSES_REMAINING_COUNTER);
    }

    public static int guessesRemaining(NoellesRoleStateComponent state) {
        return state.getCounter(GUESSES_REMAINING_COUNTER);
    }

    public static int maxGuesses(NoellesRoleState state) {
        return state.getCounter(MAX_GUESSES_COUNTER);
    }

    public static int maxGuesses(NoellesRoleStateComponent state) {
        return state.getCounter(MAX_GUESSES_COUNTER);
    }

    public static boolean isGuessableRole(Role role, boolean roleEnabled) {
        return StrawRoleMeaning.isAssassinGuessableRole(role, roleEnabled);
    }

    public static GuessAttempt evaluate(GuessInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return GuessAttempt.blocked(ValidationResult.NOT_IN_ACTIVE_ROUND);
        }
        if (!input.assassinRole()) {
            return GuessAttempt.blocked(ValidationResult.NOT_ASSASSIN);
        }
        if (!input.assassinAlive()) {
            return GuessAttempt.blocked(ValidationResult.ASSASSIN_NOT_ALIVE);
        }
        if (!input.targetExists() || input.actorUuid().equals(input.targetUuid())) {
            return GuessAttempt.blocked(ValidationResult.INVALID_TARGET);
        }
        if (!input.targetAssigned() || !input.targetAlive()) {
            return GuessAttempt.blocked(ValidationResult.TARGET_NOT_ACTIVE);
        }
        if (!input.cooldownReady()) {
            return GuessAttempt.blocked(ValidationResult.COOLDOWN);
        }
        if (input.guessesRemaining() <= 0) {
            return GuessAttempt.blocked(ValidationResult.NO_GUESSES);
        }
        if (!input.guessedRoleAllowed() || input.guessedRoleId() == null || input.targetRoleId() == null) {
            return GuessAttempt.blocked(ValidationResult.INVALID_GUESSED_ROLE);
        }
        Resolution resolution = input.targetRoleId().equals(input.guessedRoleId())
                ? Resolution.CORRECT
                : Resolution.WRONG;
        return new GuessAttempt(ValidationResult.ALLOWED, Optional.of(resolution));
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_ASSASSIN,
        ASSASSIN_NOT_ALIVE,
        INVALID_TARGET,
        TARGET_NOT_ACTIVE,
        COOLDOWN,
        NO_GUESSES,
        INVALID_GUESSED_ROLE;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public enum Resolution {
        CORRECT,
        WRONG
    }

    public record GuessAttempt(ValidationResult result, Optional<Resolution> resolution) {
        public GuessAttempt {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(resolution, "resolution");
        }

        private static GuessAttempt blocked(ValidationResult result) {
            return new GuessAttempt(result, Optional.empty());
        }
    }

    public record GuessInput(
            boolean roundRunning,
            boolean assassinRole,
            boolean assassinAlive,
            UUID actorUuid,
            UUID targetUuid,
            boolean targetExists,
            boolean targetAlive,
            boolean targetAssigned,
            boolean cooldownReady,
            int guessesRemaining,
            boolean guessedRoleAllowed,
            Identifier guessedRoleId,
            Identifier targetRoleId
    ) {
        public GuessInput {
            Objects.requireNonNull(actorUuid, "actorUuid");
            Objects.requireNonNull(targetUuid, "targetUuid");
        }

        public GuessInput withRoundRunning(boolean value) {
            return new GuessInput(value, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withAssassinRole(boolean value) {
            return new GuessInput(roundRunning, value, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withAssassinAlive(boolean value) {
            return new GuessInput(roundRunning, assassinRole, value, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withTargetUuid(UUID value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, value, targetExists,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withTargetExists(boolean value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, value,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withTargetAlive(boolean value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    value, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withTargetAssigned(boolean value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, value, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withCooldownReady(boolean value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, value, guessesRemaining, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withGuessesRemaining(int value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, cooldownReady, value, guessedRoleAllowed,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withGuessedRoleAllowed(boolean value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, value,
                    guessedRoleId, targetRoleId);
        }

        public GuessInput withGuessedRoleId(Identifier value) {
            return new GuessInput(roundRunning, assassinRole, assassinAlive, actorUuid, targetUuid, targetExists,
                    targetAlive, targetAssigned, cooldownReady, guessesRemaining, guessedRoleAllowed,
                    value, targetRoleId);
        }
    }
}
