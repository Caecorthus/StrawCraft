package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporterShopLoadoutTest {
    @Test
    void buildHandlerAddsOneOfficialWatheNotePurchase() {
        StrawShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);

        List<ShopEntry> entries = ReporterShopLoadout.withReporterNoteEntry(
                ReporterShopLoadout.withReporterNoteEntry(List.of(knife), () -> null),
                () -> null
        );

        assertEquals(List.of("knife", ReporterShopLoadout.REPORTER_NOTE_ENTRY_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
        ShopEntry note = entries.get(1);
        assertEquals(25, note.price());
        assertEquals(ShopEntry.Type.TOOL, note.type());
    }

    @Test
    void productionEntryUsesOfficialWatheNoteInsteadOfSparkOnlyItems() throws java.io.IOException {
        String src = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/ReporterShopLoadout.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(src.contains("WatheItems.NOTE.getDefaultStack()"));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }
}
