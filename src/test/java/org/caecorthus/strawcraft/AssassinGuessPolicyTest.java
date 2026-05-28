package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssassinGuessPolicyTest {
    private static final UUID ASSASSIN = new UUID(0, 1);
    private static final UUID TARGET = new UUID(0, 2);

    @Test
    void correctAndWrongGuessesResolveAgainstServerTargetRole() {
        assertEquals(
                AssassinGuessPolicy.Resolution.CORRECT,
                AssassinGuessPolicy.evaluate(validGuess()).resolution().orElseThrow()
        );
        assertEquals(
                AssassinGuessPolicy.Resolution.WRONG,
                AssassinGuessPolicy.evaluate(validGuess().withGuessedRoleId(StrawCraft.id("engineer"))).resolution().orElseThrow()
        );
    }

    @Test
    void forgedOrStaleActorAndTargetStatesCannotBypassServerGuards() {
        assertBlocked(AssassinGuessPolicy.ValidationResult.NOT_IN_ACTIVE_ROUND, validGuess().withRoundRunning(false));
        assertBlocked(AssassinGuessPolicy.ValidationResult.NOT_ASSASSIN, validGuess().withAssassinRole(false));
        assertBlocked(AssassinGuessPolicy.ValidationResult.ASSASSIN_NOT_ALIVE, validGuess().withAssassinAlive(false));
        assertBlocked(AssassinGuessPolicy.ValidationResult.INVALID_TARGET, validGuess().withTargetExists(false));
        assertBlocked(AssassinGuessPolicy.ValidationResult.INVALID_TARGET, validGuess().withTargetUuid(ASSASSIN));
        assertBlocked(AssassinGuessPolicy.ValidationResult.TARGET_NOT_ACTIVE, validGuess().withTargetAlive(false));
        assertBlocked(AssassinGuessPolicy.ValidationResult.TARGET_NOT_ACTIVE, validGuess().withTargetAssigned(false));
    }

    @Test
    void cooldownGuessesAndInvalidGuessedRoleBlockResolution() {
        assertBlocked(AssassinGuessPolicy.ValidationResult.COOLDOWN, validGuess().withCooldownReady(false));
        assertBlocked(AssassinGuessPolicy.ValidationResult.NO_GUESSES, validGuess().withGuessesRemaining(0));
        assertBlocked(AssassinGuessPolicy.ValidationResult.INVALID_GUESSED_ROLE, validGuess().withGuessedRoleAllowed(false));
    }

    @Test
    void onlyEnabledSupportedNonKillerRolesAreGuessable() {
        assertTrue(AssassinGuessPolicy.isGuessableRole(role(WatheRoleIds.CIVILIAN, true, false), true));
        assertTrue(AssassinGuessPolicy.isGuessableRole(noellesRole("reporter", true, false), true));

        assertFalse(AssassinGuessPolicy.isGuessableRole(role(WatheRoleIds.CIVILIAN, true, false), false));
        assertFalse(AssassinGuessPolicy.isGuessableRole(role(WatheRoleIds.LOOSE_END, false, false), true));
        assertFalse(AssassinGuessPolicy.isGuessableRole(role(WatheRoleIds.KILLER, false, true), true));
        assertFalse(AssassinGuessPolicy.isGuessableRole(role(WatheRoleIds.VIGILANTE, true, false), true));
        assertFalse(AssassinGuessPolicy.isGuessableRole(noellesRole("assassin", false, true), true));
        assertFalse(AssassinGuessPolicy.isGuessableRole(noellesRole("spiritualist", true, false), true));
        assertFalse(AssassinGuessPolicy.isGuessableRole(noellesRole("jester", false, false), true));
        assertFalse(AssassinGuessPolicy.isGuessableRole(noellesRole("unknown_guest", true, false), true));
    }

    @Test
    void acceptedGuessConsumesOneGuessAndStartsCooldown() {
        NoellesRoleState state = new NoellesRoleState();
        AssassinGuessPolicy.resetRoundState(state, 8, 100L);
        state.clearAbilityCooldown(AssassinGuessPolicy.ABILITY_ID);

        assertTrue(AssassinGuessPolicy.canGuess(state, 100L));
        AssassinGuessPolicy.useGuess(state, 100L);

        assertEquals(7, AssassinGuessPolicy.guessesRemaining(state));
        assertEquals(8, AssassinGuessPolicy.maxGuesses(state));
        assertTrue(state.isAbilityOnCooldown(AssassinGuessPolicy.ABILITY_ID, 100L));
        assertFalse(AssassinGuessPolicy.canGuess(state, 100L));
    }

    private static void assertBlocked(
            AssassinGuessPolicy.ValidationResult expected,
            AssassinGuessPolicy.GuessInput input
    ) {
        AssassinGuessPolicy.GuessAttempt attempt = AssassinGuessPolicy.evaluate(input);

        assertEquals(expected, attempt.result());
        assertTrue(attempt.result().blocked());
        assertTrue(attempt.resolution().isEmpty());
    }

    private static AssassinGuessPolicy.GuessInput validGuess() {
        return new AssassinGuessPolicy.GuessInput(
                true,
                true,
                true,
                ASSASSIN,
                TARGET,
                true,
                true,
                true,
                true,
                2,
                true,
                StrawCraft.id("reporter"),
                StrawCraft.id("reporter")
        );
    }

    private static Role noellesRole(String path, boolean innocent, boolean killerTools) {
        return new Role(StrawCraft.id(path), 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }

    private static Role role(Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
