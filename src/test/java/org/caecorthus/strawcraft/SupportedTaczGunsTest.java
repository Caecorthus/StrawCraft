package org.caecorthus.strawcraft;

import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class SupportedTaczGunsTest {
    @Test
    void p320KeepsGunStackAndAmmoProfileIdentityTogether() {
        SupportedTaczGun p320 = SupportedTaczGuns.P320;

        assertSame(TaczGunProfiles.P320, p320.profile());
        assertEquals(TaczGunProfiles.P320.gunId(), p320.gunId());
        assertEquals(TaczGunProfiles.P320.ammoId(), p320.ammoId());
        assertSame(p320, SupportedTaczGuns.gunFor(TaczGunProfiles.P320.gunId()).orElseThrow());
    }

    @Test
    void p320CreatesSameTaczGunCustomDataAsLegacyStackHelper() {
        NbtComponent customData = SupportedTaczGuns.P320.createGunCustomData();
        NbtComponent legacyCustomData = TaczGunStacks.createGunCustomData(TaczGunProfiles.P320);
        NbtCompound nbt = customData.copyNbt();

        assertEquals(legacyCustomData.copyNbt(), nbt);
        assertEquals("tacz:p320", nbt.getString("GunId"));
        assertFalse(nbt.contains("GunCurrentAmmoCount"));
        assertFalse(nbt.contains("DummyAmmo"));
        assertFalse(nbt.contains("MaxDummyAmmo"));
    }
}
