package org.caecorthus.strawcraft;

import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczGunProfilesTest {
    @Test
    void supportedHandgunsUseFloorOneThirdLowAmmoThreshold() {
        TaczGunProfile rhino357 = TaczGunProfiles.profileFor(Identifier.of("tacz", "rhino357")).orElseThrow();
        TaczGunProfile p320 = TaczGunProfiles.profileFor(Identifier.of("tacz", "p320")).orElseThrow();

        assertTrue(rhino357.isLowAmmo(2));
        assertFalse(rhino357.isLowAmmo(3));
        assertTrue(p320.isLowAmmo(4));
        assertFalse(p320.isLowAmmo(5));
    }

    @Test
    void refillAmmoCountIsOnlyTheMissingMagazineAmmo() {
        TaczGunProfile p320 = TaczGunProfiles.profileFor(Identifier.of("tacz", "p320")).orElseThrow();

        assertEquals(8, p320.missingAmmo(4));
        assertEquals(0, p320.missingAmmo(12));
        assertEquals(0, p320.missingAmmo(13));
    }

    @Test
    void ammoStacksUseTaczAmmoItemWithAmmoIdCustomData() {
        NbtComponent customData = TaczGunStacks.createAmmoCustomData(Identifier.of("tacz", "357mag"));
        NbtCompound nbt = customData.copyNbt();

        assertEquals("tacz:357mag", nbt.getString("AmmoId"));
    }
}
