package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaHealthBridgeTest {
    @Test
    void watheKillRequestsWithDamageBecomeVanillaDamageAndCancelWatheKill() {
        VanillaHealthBridge.KillRequestPlan plan =
                VanillaHealthBridge.planKillRequest(GameConstants.DeathReasons.KNIFE, true, true);

        assertTrue(plan.cancelsWatheKill());
        assertTrue(plan.appliesVanillaDamage());
        assertEquals(14.0f, plan.damage());
        assertEquals(VanillaHealthBridge.DamageSourceKind.PLAYER_ATTACK, plan.damageSourceKind());
    }

    @Test
    void watheGunKillRequestsAreCancelledWithoutApplyingExtraDamage() {
        VanillaHealthBridge.KillRequestPlan plan =
                VanillaHealthBridge.planKillRequest(GameConstants.DeathReasons.GUN, false, true);

        assertTrue(plan.cancelsWatheKill());
        assertFalse(plan.appliesVanillaDamage());
        assertEquals(0.0f, plan.damage());
    }

    @Test
    void vanillaDeathKillRequestsFallThroughToWatheDeathPipeline() {
        VanillaHealthBridge.KillRequestPlan plan =
                VanillaHealthBridge.planKillRequest(GameConstants.DeathReasons.VANILLA_DEATH, false, false);

        assertFalse(plan.cancelsWatheKill());
        assertFalse(plan.appliesVanillaDamage());
        assertEquals(0.0f, plan.damage());
    }

    @Test
    void vanillaDeathBookkeepingOnlyMarksLiveWatheRoundPlayers() {
        assertTrue(VanillaHealthBridge.shouldMarkPlayerDeadAfterVanillaDeath(true, true, false));
        assertFalse(VanillaHealthBridge.shouldMarkPlayerDeadAfterVanillaDeath(false, true, false));
        assertFalse(VanillaHealthBridge.shouldMarkPlayerDeadAfterVanillaDeath(true, false, false));
        assertFalse(VanillaHealthBridge.shouldMarkPlayerDeadAfterVanillaDeath(true, true, true));
    }
}
