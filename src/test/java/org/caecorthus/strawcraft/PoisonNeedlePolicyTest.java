package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoisonNeedlePolicyTest {
    @Test
    void poisonerCanNeedleAliveSurvivalUnpoisonedPlayer() {
        PoisonNeedlePolicy.Decision decision = PoisonNeedlePolicy.evaluateUse(new PoisonNeedlePolicy.Input(
                role("poisoner", true, false),
                true,
                true,
                true,
                true,
                false,
                0,
                true
        ));

        assertTrue(decision.allowed());
        assertEquals(PoisonNeedlePolicy.POISON_TICKS, decision.poisonTicksAfterUse());
    }

    @Test
    void nonPoisonerDeadOrNonSurvivalTargetsAreRejected() {
        assertFalse(decisionFor(role("killer", true, false), true, true, 0).allowed());
        assertFalse(decisionFor(role("poisoner", true, false), false, true, 0).allowed());
        assertFalse(decisionFor(role("poisoner", true, false), true, false, 0).allowed());
    }

    @Test
    void existingPoisonTicksArePreservedInsteadOfExtendedOrCleared() {
        PoisonNeedlePolicy.Decision decision = decisionFor(role("poisoner", true, false), true, true, 2_000);

        assertFalse(decision.allowed());
        assertEquals(2_000, decision.poisonTicksAfterUse());
    }

    @Test
    void cooldownMustBeReadyBeforeNeedleCanApplyPoison() {
        PoisonNeedlePolicy.Decision decision = PoisonNeedlePolicy.evaluateUse(new PoisonNeedlePolicy.Input(
                role("poisoner", true, false),
                true,
                true,
                true,
                true,
                false,
                0,
                false
        ));

        assertFalse(decision.allowed());
        assertEquals(0, decision.poisonTicksAfterUse());
    }

    private static PoisonNeedlePolicy.Decision decisionFor(Role role, boolean targetAlive, boolean targetSurvival, int poisonTicks) {
        return PoisonNeedlePolicy.evaluateUse(new PoisonNeedlePolicy.Input(
                role,
                true,
                true,
                targetAlive,
                targetSurvival,
                false,
                poisonTicks,
                true
        ));
    }

    private static Role role(String path, boolean killerTools, boolean innocent) {
        return new Role(StrawCraft.id(path), 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
