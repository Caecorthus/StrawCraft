package org.caecorthus.strawcraft;

import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczGunProfilesTest {
    @Test
    void supportedHandgunsUseFloorOneThirdLowAmmoThreshold() {
        TaczGunProfile rhino357 = TaczGunProfiles.RHINO357;
        TaczGunProfile p320 = TaczGunProfiles.P320;

        assertTrue(rhino357.isLowAmmo(2));
        assertFalse(rhino357.isLowAmmo(3));
        assertTrue(p320.isLowAmmo(4));
        assertFalse(p320.isLowAmmo(5));
    }

    @Test
    void refillAmmoCountIsOnlyTheMissingMagazineAmmo() {
        TaczGunProfile p320 = TaczGunProfiles.P320;

        assertEquals(8, p320.missingAmmo(4));
        assertEquals(0, p320.missingAmmo(12));
        assertEquals(0, p320.missingAmmo(13));
    }

    @Test
    void ammoStacksUseTaczAmmoItemWithAmmoIdCustomData() {
        NbtComponent customData = TaczGunStacks.createAmmoCustomData(TaczGunProfiles.RHINO357);
        NbtCompound nbt = customData.copyNbt();

        assertEquals("tacz:357mag", nbt.getString("AmmoId"));
    }

    @Test
    void gunStacksUseProfileGunIdCustomData() {
        NbtComponent customData = TaczGunStacks.createGunCustomData(TaczGunProfiles.RHINO357);
        NbtCompound nbt = customData.copyNbt();

        assertEquals("tacz:rhino357", nbt.getString("GunId"));
    }
}
