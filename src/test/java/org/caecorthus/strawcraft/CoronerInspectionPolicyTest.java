package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CoronerInspectionPolicyTest {
    @Test
    void validationCoversServerOwnedCoronerBodyInspectionChecks() {
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.ALLOWED,
                CoronerInspectionPolicy.validateInteraction(input(true, true, true, true, 24.9D, true, false))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.NO_BODY,
                CoronerInspectionPolicy.validateInteraction(input(true, true, true, false, 24.9D, true, false))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.OUT_OF_RANGE,
                CoronerInspectionPolicy.validateInteraction(input(true, true, true, true, 25.1D, true, false))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.NO_METADATA,
                CoronerInspectionPolicy.validateInteraction(input(true, true, true, true, 24.9D, false, false))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.HIDDEN_BODY,
                CoronerInspectionPolicy.validateInteraction(input(true, true, true, true, 24.9D, true, true))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.NOT_CORONER,
                CoronerInspectionPolicy.validateInteraction(input(true, false, true, true, 24.9D, true, false))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.NOT_ALIVE,
                CoronerInspectionPolicy.validateInteraction(input(true, true, false, true, 24.9D, true, false))
        );
        assertEquals(
                CoronerInspectionPolicy.ValidationResult.NOT_RUNNING,
                CoronerInspectionPolicy.validateInteraction(input(false, true, true, true, 24.9D, true, false))
        );
    }

    @Test
    void inspectionReportsWatheDeathReasonAndElapsedTimeWithoutKillerReveal() {
        UUID killer = UUID.randomUUID();
        StrawCorpseMetadata.CorpseMetadata metadata = new StrawCorpseMetadata.CorpseMetadata(
                UUID.randomUUID(),
                Optional.of(killer),
                false,
                GameConstants.DeathReasons.KNIFE,
                100L,
                true,
                true
        );

        CoronerInspectionPolicy.InspectionResult result = CoronerInspectionPolicy.inspect(metadata, 165L);

        assertEquals(GameConstants.DeathReasons.KNIFE, result.deathReason());
        assertEquals(65L, result.elapsedTicks());
        assertEquals(3L, result.elapsedSeconds());
        assertEquals(
                "death_reason=" + GameConstants.DeathReasons.KNIFE + ";elapsed_ticks=65;elapsed_seconds=3",
                CoronerInspectionPolicy.stableSummary(result)
        );
        assertFalse(CoronerInspectionPolicy.stableSummary(result).contains(killer.toString()));
    }

    @Test
    void elapsedTimeIsNeverNegativeForLateMetadataArrival() {
        StrawCorpseMetadata.CorpseMetadata metadata = new StrawCorpseMetadata.CorpseMetadata(
                UUID.randomUUID(),
                Optional.empty(),
                false,
                GameConstants.DeathReasons.GUN,
                100L,
                true,
                true
        );

        CoronerInspectionPolicy.InspectionResult result = CoronerInspectionPolicy.inspect(metadata, 90L);

        assertEquals(0L, result.elapsedTicks());
        assertEquals(0L, result.elapsedSeconds());
    }

    private static CoronerInspectionPolicy.InteractionInput input(
            boolean gameRunning,
            boolean coronerRole,
            boolean playerAlive,
            boolean bodyFound,
            double nearestBodyDistanceSquared,
            boolean metadataPresent,
            boolean hiddenBody
    ) {
        return new CoronerInspectionPolicy.InteractionInput(
                gameRunning,
                coronerRole,
                playerAlive,
                bodyFound,
                nearestBodyDistanceSquared,
                metadataPresent,
                hiddenBody
        );
    }
}
