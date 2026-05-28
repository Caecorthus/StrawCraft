package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScavengerShopLoadoutTest {
    @Test
    void buildHandlerAddsOneRealClockPurchaseWithKnifeCooldownResetBehavior() {
        StrawShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);

        List<ShopEntry> entries = ScavengerShopLoadout.withResetKnifeCooldownEntry(
                ScavengerShopLoadout.withResetKnifeCooldownEntry(List.of(knife), () -> null),
                () -> null
        );
        assertEquals(List.of("knife", ScavengerShopLoadout.RESET_KNIFE_COOLDOWN_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
        ShopEntry reset = entries.get(1);
        assertEquals(150, reset.price());
        assertEquals(ShopEntry.Type.WEAPON, reset.type());
    }

    @Test
    void productionResetEntryUsesMinecraftClockAndWatheKnifeCooldown() throws java.io.IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/ScavengerShopLoadout.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("Items.CLOCK.getDefaultStack()"));
        assertTrue(source.contains("cooldowns.remove(WatheItems.KNIFE)"));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }
}
