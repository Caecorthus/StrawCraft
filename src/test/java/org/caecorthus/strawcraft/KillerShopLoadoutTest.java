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
        ShopEntry original = new ShopEntry.Builder("revolver", null, 300, ShopEntry.Type.WEAPON).build();

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
        ShopEntry knife = new ShopEntry.Builder("knife", null, 100, ShopEntry.Type.WEAPON)
                .stock(1)
                .build();
        ShopEntry revolver = new ShopEntry.Builder("revolver", null, 300, ShopEntry.Type.WEAPON)
                .cooldown(40)
                .initialCooldown(20)
                .stock(2)
                .build();
        ShopEntry grenade = new ShopEntry.Builder("grenade", null, 350, ShopEntry.Type.WEAPON)
                .build();
        ShopEntry derringer = new ShopEntry.Builder("derringer", null, 200, ShopEntry.Type.WEAPON)
                .build();

        List<ShopEntry> rewritten = KillerShopLoadout.rewriteEntries(
                List.of(knife, revolver, grenade, derringer),
                KillerShopLoadoutTest::p320ReplacementFor,
                entry -> "derringer".equals(entry.id())
        );

        assertEquals(3, rewritten.size());
        assertSame(knife, rewritten.get(0));
        assertSame(grenade, rewritten.get(2));

        ShopEntry replacement = rewritten.get(1);
        assertEquals("p320", replacement.id());
        assertSame(ShopEntry.Type.WEAPON, replacement.type());
        assertEquals(300, replacement.price());
        assertEquals(40, replacement.cooldownTicks());
        assertEquals(20, replacement.initialCooldownTicks());
        assertEquals(2, replacement.maxStock());
    }

    private static Optional<ShopEntry> p320ReplacementFor(ShopEntry original) {
        KillerShopLoadout.ReplacementSettings settings = KillerShopLoadout.replacementSettingsFor(original);
        ShopEntry.Builder builder = new ShopEntry.Builder(settings.id(), null, settings.price(), settings.type());
        if (settings.hasCooldown()) {
            builder.cooldown(settings.cooldownTicks());
        }
        if (settings.hasInitialCooldown()) {
            builder.initialCooldown(settings.initialCooldownTicks());
        }
        if (settings.hasStockLimit()) {
            builder.stock(settings.maxStock());
        }
        return Optional.of(builder.build());
    }
}
