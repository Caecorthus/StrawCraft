package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class KillerShopCatalogTest {
    @Test
    void killerGunReplacementUsesSupportedP320Module() {
        assertSame(SupportedTaczGuns.P320, KillerShopCatalog.KILLER_SUPPORTED_GUN);
        assertSame(TaczGunProfiles.P320, KillerShopCatalog.KILLER_GUN);
    }

    @Test
    void materializedCatalogKeepsWatheIndexesWhenRevolverBecomesP320() {
        ShopEntry knife = new ShopEntry(null, 100, ShopEntry.Type.WEAPON);
        ShopEntry revolver = new StrawShopEntry("revolver", null, null, 300, ShopEntry.Type.WEAPON, 40, 20, 2);
        ShopEntry poison = new ShopEntry(null, 250, ShopEntry.Type.POISON);

        KillerShopCatalog catalog = KillerShopCatalog.materialize(
                List.of(knife, revolver, poison),
                entry -> entry == revolver,
                KillerShopCatalogTest::p320ReplacementFor,
                entry -> false
        );

        assertEquals(List.of(100, 300, 250), catalog.entries().stream().map(ShopEntry::price).toList());
        assertEquals(List.of(0, 1, 2), IntStream.range(0, catalog.entries().size())
                .map(catalog::originalPurchaseIndexAt)
                .boxed()
                .toList());

        ShopEntry replacement = catalog.entryForOriginalPurchaseIndex(1).orElseThrow();
        assertSame(catalog.entries().get(1), replacement);
        StrawShopEntry p320 = assertInstanceOf(StrawShopEntry.class, replacement);
        assertEquals("p320", p320.id());
        assertSame(p320.displayStack(), p320.actualStack());
        assertEquals(300, p320.price());
        assertEquals(40, p320.cooldownTicks());
        assertEquals(20, p320.initialCooldownTicks());
        assertEquals(2, p320.maxStock());
    }

    private static Optional<ShopEntry> p320ReplacementFor(ShopEntry original) {
        KillerShopCatalog.ReplacementSettings settings = KillerShopCatalog.replacementSettingsFor(original);
        return Optional.of(new StrawShopEntry(
                settings.id(),
                null,
                null,
                settings.price(),
                settings.type(),
                settings.cooldownTicks(),
                settings.initialCooldownTicks(),
                settings.maxStock()
        ));
    }
}
