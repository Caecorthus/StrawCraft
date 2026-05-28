package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

public final class MermaidWaterAdaptationPolicy {
    public static final Identifier MERMAID_ROLE = StrawCraft.id("mermaid");
    public static final int EFFECT_DURATION_TICKS = 260;
    public static final double MAX_SPRINT_TIME_BONUS_TICKS = 200.0D;

    private MermaidWaterAdaptationPolicy() {
    }

    public static Result evaluate(Input input) {
        return input.roundRunning()
                && input.mermaid()
                && input.aliveAndSurvival()
                && input.touchingWater()
                ? Result.ACTIVE
                : Result.CLEANUP;
    }

    public record Input(
            boolean roundRunning,
            boolean mermaid,
            boolean aliveAndSurvival,
            boolean touchingWater
    ) {
    }

    public enum Result {
        ACTIVE,
        CLEANUP
    }
}
