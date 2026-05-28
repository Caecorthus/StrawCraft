package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Objects;

public final class CoronerInspectionPolicy {
    public static final Identifier CORONER_ROLE = StrawCraft.id("coroner");
    public static final double INSPECT_RANGE = 5.0D;
    public static final double INSPECT_RANGE_SQUARED = INSPECT_RANGE * INSPECT_RANGE;

    private CoronerInspectionPolicy() {
    }

    public static ValidationResult validateInteraction(InteractionInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.gameRunning()) {
            return ValidationResult.NOT_RUNNING;
        }
        if (!input.coronerRole()) {
            return ValidationResult.NOT_CORONER;
        }
        if (!input.playerAlive()) {
            return ValidationResult.NOT_ALIVE;
        }
        if (!input.bodyFound()) {
            return ValidationResult.NO_BODY;
        }
        if (input.nearestBodyDistanceSquared() > INSPECT_RANGE_SQUARED) {
            return ValidationResult.OUT_OF_RANGE;
        }
        if (!input.metadataPresent()) {
            return ValidationResult.NO_METADATA;
        }
        if (input.hiddenBody()) {
            return ValidationResult.HIDDEN_BODY;
        }
        return ValidationResult.ALLOWED;
    }

    public static InspectionResult inspect(StrawCorpseMetadata.CorpseMetadata metadata, long currentGameTime) {
        Objects.requireNonNull(metadata, "metadata");
        return new InspectionResult(
                metadata.deathReason(),
                Math.max(0L, currentGameTime - metadata.gameTime())
        );
    }

    public static String stableSummary(InspectionResult result) {
        Objects.requireNonNull(result, "result");
        return "death_reason=" + result.deathReason()
                + ";elapsed_ticks=" + result.elapsedTicks()
                + ";elapsed_seconds=" + result.elapsedSeconds();
    }

    public record InteractionInput(
            boolean gameRunning,
            boolean coronerRole,
            boolean playerAlive,
            boolean bodyFound,
            double nearestBodyDistanceSquared,
            boolean metadataPresent,
            boolean hiddenBody
    ) {
    }

    public record InspectionResult(
            Identifier deathReason,
            long elapsedTicks
    ) {
        public long elapsedSeconds() {
            return elapsedTicks / 20L;
        }
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_RUNNING,
        NOT_CORONER,
        NOT_ALIVE,
        NO_BODY,
        OUT_OF_RANGE,
        NO_METADATA,
        HIDDEN_BODY
    }
}
