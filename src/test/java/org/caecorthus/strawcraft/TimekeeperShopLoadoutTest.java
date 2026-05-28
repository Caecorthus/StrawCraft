package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimekeeperShopLoadoutTest {
    @Test
    void buildHandlerAddsOneClockPurchaseForSubtractingGameTime() {
        StrawShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);
        List<ShopEntry> entries = TimekeeperShopLoadout.withTimeSubtractionEntry(
                TimekeeperShopLoadout.withTimeSubtractionEntry(List.of(knife), () -> null),
                () -> null
        );

        assertEquals(List.of("knife", TimekeeperShopLoadout.TIME_SUBTRACTION_ENTRY_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());

        ShopEntry timer = entries.get(1);
        assertEquals(100, timer.price());
        assertEquals(ShopEntry.Type.TOOL, timer.type());
    }

    @Test
    void purchaseSubtractsSmallFixedAmountFromOfficialTimer() {
        AtomicInteger adjustment = new AtomicInteger();

        assertTrue(TimekeeperShopLoadout.subtractTime(adjustment::addAndGet));

        assertEquals(-900, adjustment.get());
    }

    @Test
    void productionEntryUsesMinecraftClockAndOfficialWatheTimerComponent() throws java.io.IOException {
        String src = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/TimekeeperShopLoadout.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(src.contains("Items.CLOCK.getDefaultStack()"));
        assertTrue(src.contains("GameTimeComponent.KEY.get(player.getWorld()).addTime"));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }
}
