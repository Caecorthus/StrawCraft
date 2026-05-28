package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathogenInfectionPolicyTest {
    private static final UUID PATHOGEN = new UUID(0, 10);
    private static final UUID FAR = new UUID(0, 20);
    private static final UUID NEAREST_HIGH_UUID = new UUID(0, 30);
    private static final UUID NEAREST_LOW_UUID = new UUID(0, 25);

    @Test
    void baseCooldownScalesFromStartingPlayerCount() {
        assertEquals(7 * 20, PathogenInfectionPolicy.baseCooldownTicks(25));
        assertEquals(10 * 20, PathogenInfectionPolicy.baseCooldownTicks(24));
        assertEquals(10 * 20, PathogenInfectionPolicy.baseCooldownTicks(18));
        assertEquals(15 * 20, PathogenInfectionPolicy.baseCooldownTicks(17));
        assertEquals(15 * 20, PathogenInfectionPolicy.baseCooldownTicks(12));
        assertEquals(20 * 20, PathogenInfectionPolicy.baseCooldownTicks(11));
    }

    @Test
    void forgedOrStaleStatesCannotBypassServerGuards() {
        assertBlocked(validActivation().withRoundRunning(false));
        assertBlocked(validActivation().withPathogenRole(false));
        assertBlocked(validActivation().withPathogenAlive(false));
        assertEquals(
                PathogenInfectionPolicy.ValidationResult.COOLDOWN,
                PathogenInfectionPolicy.evaluate(validActivation().withCooldownReady(false)).result()
        );
        assertEquals(
                PathogenInfectionPolicy.ValidationResult.NO_TARGET,
                PathogenInfectionPolicy.evaluate(validActivation().withCandidates(List.of())).result()
        );
    }

    @Test
    void choosesNearestValidVisibleUninfectedTargetDeterministically() {
        PathogenInfectionPolicy.ActivationInput input = validActivation().withCandidates(List.of(
                target(FAR, 8.0D),
                target(PATHOGEN, 1.0D),
                target(UUID.randomUUID(), 2.0D).withAssigned(false),
                target(UUID.randomUUID(), 2.0D).withAlive(false),
                target(UUID.randomUUID(), 2.0D).withInfected(true),
                target(UUID.randomUUID(), 9.01D),
                target(UUID.randomUUID(), 2.0D).withVisible(false),
                target(NEAREST_HIGH_UUID, 2.0D),
                target(NEAREST_LOW_UUID, 2.0D)
        ));

        PathogenInfectionPolicy.InfectionAttempt attempt = PathogenInfectionPolicy.evaluate(input);

        assertEquals(PathogenInfectionPolicy.ValidationResult.ALLOWED, attempt.result());
        assertEquals(Optional.of(NEAREST_LOW_UUID), attempt.targetUuid());
    }

    @Test
    void rejectsInvalidTargetWhenAllCandidatesAreSelfDeadUnassignedInfectedOutOfRangeOrHidden() {
        PathogenInfectionPolicy.ActivationInput input = validActivation().withCandidates(List.of(
                target(PATHOGEN, 1.0D),
                target(UUID.randomUUID(), 1.0D).withAssigned(false),
                target(UUID.randomUUID(), 1.0D).withAlive(false),
                target(UUID.randomUUID(), 1.0D).withInfected(true),
                target(UUID.randomUUID(), 9.01D),
                target(UUID.randomUUID(), 1.0D).withVisible(false)
        ));

        assertEquals(
                PathogenInfectionPolicy.ValidationResult.NO_TARGET,
                PathogenInfectionPolicy.evaluate(input).result()
        );
    }

    private static void assertBlocked(PathogenInfectionPolicy.ActivationInput input) {
        assertTrue(PathogenInfectionPolicy.evaluate(input).result().blocked());
    }

    private static PathogenInfectionPolicy.ActivationInput validActivation() {
        return new PathogenInfectionPolicy.ActivationInput(
                true,
                true,
                true,
                true,
                PATHOGEN,
                List.of(target(FAR, 4.0D))
        );
    }

    private static PathogenInfectionPolicy.TargetCandidate target(UUID uuid, double squaredDistance) {
        return new PathogenInfectionPolicy.TargetCandidate(
                uuid,
                true,
                true,
                false,
                true,
                squaredDistance
        );
    }
}
