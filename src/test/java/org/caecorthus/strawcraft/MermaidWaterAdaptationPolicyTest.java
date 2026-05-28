package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MermaidWaterAdaptationPolicyTest {
    @Test
    void livingMermaidInWaterDuringActiveRoundReceivesAdaptation() {
        assertEquals(
                MermaidWaterAdaptationPolicy.Result.ACTIVE,
                MermaidWaterAdaptationPolicy.evaluate(new MermaidWaterAdaptationPolicy.Input(
                        true,
                        true,
                        true,
                        true
                ))
        );
    }

    @Test
    void anyMissingEligibilityCleansUpAdaptation() {
        assertEquals(MermaidWaterAdaptationPolicy.Result.CLEANUP, result(false, true, true, true));
        assertEquals(MermaidWaterAdaptationPolicy.Result.CLEANUP, result(true, false, true, true));
        assertEquals(MermaidWaterAdaptationPolicy.Result.CLEANUP, result(true, true, false, true));
        assertEquals(MermaidWaterAdaptationPolicy.Result.CLEANUP, result(true, true, true, false));
    }

    private static MermaidWaterAdaptationPolicy.Result result(
            boolean roundRunning,
            boolean mermaid,
            boolean aliveAndSurvival,
            boolean touchingWater
    ) {
        return MermaidWaterAdaptationPolicy.evaluate(new MermaidWaterAdaptationPolicy.Input(
                roundRunning,
                mermaid,
                aliveAndSurvival,
                touchingWater
        ));
    }
}
