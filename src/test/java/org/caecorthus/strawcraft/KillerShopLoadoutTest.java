package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KillerShopLoadoutTest {
    @Test
    void killerP320CustomDataIdentifiesTaczGunWithoutBundledAmmo() {
        NbtComponent component = KillerShopLoadout.createP320CustomData();
        NbtCompound customData = component.copyNbt();

        assertEquals("tacz:p320", customData.getString("GunId"));
        assertFalse(customData.contains("GunCurrentAmmoCount"));
        assertFalse(customData.contains("DummyAmmo"));
        assertFalse(customData.contains("MaxDummyAmmo"));
    }

    @Test
    void p320ReplacementKeepsOriginalKillerShopPricingAndType() {
        ShopEntry original = new ShopEntry.Builder("revolver", null, 300, ShopEntry.Type.WEAPON).build();

        KillerShopLoadout.ReplacementSettings settings = KillerShopLoadout.replacementSettingsFor(original);

        assertEquals("p320", settings.id());
        assertEquals(300, settings.price());
        assertEquals(ShopEntry.Type.WEAPON, settings.type());
        assertFalse(settings.hasStockLimit());
        assertFalse(settings.hasCooldown());
        assertFalse(settings.hasInitialCooldown());
    }
}
