package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WatheDeathReasonTrackerTest {
    @Test
    void attributedDamageCarriesDeathReasonAndKillerOnce() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        WatheDeathReasonTracker.rememberDeathAttribution(victim, GameConstants.DeathReasons.GUN, killer);

        Optional<WatheDeathReasonTracker.DeathAttribution> attribution =
                WatheDeathReasonTracker.consumeDeathAttribution(victim, GameConstants.DeathReasons.VANILLA_DEATH);
        Optional<WatheDeathReasonTracker.DeathAttribution> consumed =
                WatheDeathReasonTracker.consumeDeathAttribution(victim, GameConstants.DeathReasons.VANILLA_DEATH);

        assertEquals(GameConstants.DeathReasons.GUN, attribution.orElseThrow().deathReason());
        assertEquals(Optional.of(killer), attribution.orElseThrow().killerUuid());
        assertEquals(GameConstants.DeathReasons.VANILLA_DEATH, consumed.orElseThrow().deathReason());
        assertEquals(Optional.empty(), consumed.orElseThrow().killerUuid());
    }

    @Test
    void attributedDamageReasonOverridesNextVanillaDeathOnce() {
        UUID victim = UUID.randomUUID();

        WatheDeathReasonTracker.rememberDeathReason(victim, GameConstants.DeathReasons.GRENADE);

        assertEquals(GameConstants.DeathReasons.GRENADE,
                WatheDeathReasonTracker.consumeDeathReason(victim, GameConstants.DeathReasons.VANILLA_DEATH));
        assertEquals(GameConstants.DeathReasons.VANILLA_DEATH,
                WatheDeathReasonTracker.consumeDeathReason(victim, GameConstants.DeathReasons.VANILLA_DEATH));
    }

    @Test
    void nonLethalAttributedDamageCanBeClearedBeforeLaterVanillaDeath() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        WatheDeathReasonTracker.rememberDeathAttribution(victim, GameConstants.DeathReasons.KNIFE, killer);
        WatheDeathReasonTracker.clearDeathReason(victim);

        WatheDeathReasonTracker.DeathAttribution attribution =
                WatheDeathReasonTracker.consumeDeathAttribution(victim, GameConstants.DeathReasons.VANILLA_DEATH).orElseThrow();

        assertEquals(GameConstants.DeathReasons.VANILLA_DEATH, attribution.deathReason());
        assertEquals(Optional.empty(), attribution.killerUuid());
    }

    @Test
    void taczBulletDamageTypesMapToWatheGunShotReason() {
        assertEquals(GameConstants.DeathReasons.GUN,
                WatheDeathReasonTracker.watheReasonForDamageType(Identifier.of("tacz", "bullet")).orElseThrow());
        assertEquals(GameConstants.DeathReasons.GUN,
                WatheDeathReasonTracker.watheReasonForDamageType(Identifier.of("tacz", "bullet_ignore_armor")).orElseThrow());
        assertEquals(GameConstants.DeathReasons.GUN,
                WatheDeathReasonTracker.watheReasonForDamageType(Identifier.of("tacz", "bullet_void")).orElseThrow());
        assertEquals(GameConstants.DeathReasons.GUN,
                WatheDeathReasonTracker.watheReasonForDamageType(Identifier.of("tacz", "bullet_void_ignore_armor")).orElseThrow());
        assertEquals(GameConstants.DeathReasons.VANILLA_DEATH,
                WatheDeathReasonTracker.watheReasonForDamageType(Identifier.of("minecraft", "lava"))
                        .orElse(GameConstants.DeathReasons.VANILLA_DEATH));
    }
}
