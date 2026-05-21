package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        ShopEntry original = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);

        KillerShopLoadout.ReplacementSettings settings = KillerShopLoadout.replacementSettingsFor(original);

        assertEquals("p320", settings.id());
        assertEquals(300, settings.price());
        assertEquals(ShopEntry.Type.WEAPON, settings.type());
        assertFalse(settings.hasStockLimit());
        assertFalse(settings.hasCooldown());
        assertFalse(settings.hasInitialCooldown());
    }

    @Test
    void rewritesWatheKillerShopEntriesWithoutReorderingSurvivors() {
        ShopEntry knife = new ShopEntry(null, 100, ShopEntry.Type.WEAPON);
        ShopEntry revolver = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);
        ShopEntry grenade = new ShopEntry(null, 350, ShopEntry.Type.WEAPON);
        ShopEntry derringer = new ShopEntry(null, 200, ShopEntry.Type.WEAPON);

        List<ShopEntry> rewritten = KillerShopLoadout.rewriteEntries(
                List.of(knife, revolver, grenade, derringer),
                entry -> entry == revolver,
                KillerShopLoadoutTest::p320ReplacementFor,
                entry -> entry == derringer
        );

        assertEquals(3, rewritten.size());
        assertSame(knife, rewritten.get(0));
        assertSame(grenade, rewritten.get(2));

        ShopEntry replacement = rewritten.get(1);
        assertSame(ShopEntry.Type.WEAPON, replacement.type());
        assertEquals(300, replacement.price());
    }

    private static Optional<ShopEntry> p320ReplacementFor(ShopEntry original) {
        KillerShopLoadout.ReplacementSettings settings = KillerShopLoadout.replacementSettingsFor(original);
        return Optional.of(new ShopEntry(null, settings.price(), settings.type()));
    }
}
