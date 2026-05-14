package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaDamagePolicyTest {
    @Test
    void knifeKillRequestsUseFrontStabDamageByDefault() {
        assertEquals(8.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.KNIFE));
    }

    @Test
    void knifeBackstabsDealAmbushDamage() {
        assertEquals(14.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.KNIFE, true));
    }

    @Test
    void watheKillRequestsUseExplicitVanillaDamageAmounts() {
        assertEquals(0.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.GUN));
        assertEquals(0.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.GUN_BACKFIRE));
        assertEquals(0.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.SHOT_INNOCENT));
        assertEquals(12.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.BAT));
        assertEquals(12.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.GRENADE));
        assertEquals(4.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.POISON));
        assertEquals(20.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.FELL_OUT_OF_TRAIN));
        assertEquals(4.0f, VanillaDamagePolicy.damageFor(GameConstants.DeathReasons.MENTAL_BREAKDOWN));
        assertEquals(0.0f, VanillaDamagePolicy.damageFor(Identifier.of("strawcraft", "unknown")));
    }
}
