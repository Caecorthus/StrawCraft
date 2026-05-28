package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaiterShopLoadoutTest {
    @Test
    void buildHandlerAddsOneWaiterServicePurchaseWithoutChangingExistingIndices() {
        StrawShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        List<ShopEntry> entries = WaiterShopLoadout.withWaiterServiceEntry(
                WaiterShopLoadout.withWaiterServiceEntry(List.of(knife), () -> null),
                () -> null
        );

        assertEquals(List.of("knife", WaiterShopLoadout.WAITER_SERVICE_ENTRY_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
        assertEquals(50, entries.get(1).price());
        assertEquals(ShopEntry.Type.TOOL, entries.get(1).type());
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }
}
