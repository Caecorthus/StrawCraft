package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BartenderDefenseVialShopLoadoutTest {
    @Test
    void buildHandlerAddsOneRealStrawCraftDefenseVialPurchase() {
        StrawShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        List<ShopEntry> entries = BartenderDefenseVialShopLoadout.withDefenseVialEntry(
                BartenderDefenseVialShopLoadout.withDefenseVialEntry(List.of(knife), () -> null),
                () -> null
        );

        assertEquals(List.of("knife", BartenderDefenseVialShopLoadout.DEFENSE_VIAL_ENTRY_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
        ShopEntry vial = entries.get(1);
        assertEquals(100, vial.price());
        assertEquals(ShopEntry.Type.TOOL, vial.type());
    }

    @Test
    void productionEntryUsesRealStrawCraftVialInsteadOfSparkOnlyItems() throws java.io.IOException {
        String src = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/BartenderDefenseVialShopLoadout.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(src.contains("StrawCraftItems.DEFENSE_VIAL.getDefaultStack()"));
        assertTrue(src.contains("item.strawcraft.\" + DEFENSE_VIAL_ENTRY_ID"));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }
}
