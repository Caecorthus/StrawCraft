package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilencerShopLoadoutTest {
    @Test
    void buildHandlerAddsOneSilentPsychoPurchaseWithoutChangingExistingIndices() {
        StrawShopEntry knife = entry("knife", 100, ShopEntry.Type.WEAPON);

        List<ShopEntry> entries = SilencerShopLoadout.withSilentPsychoEntry(
                SilencerShopLoadout.withSilentPsychoEntry(List.of(knife), () -> null),
                () -> null
        );

        assertEquals(List.of("knife", SilencerShopLoadout.SILENT_PSYCHO_ENTRY_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
        assertEquals(SilencerShopLoadout.SILENT_PSYCHO_PRICE, entries.get(1).price());
        assertEquals(ShopEntry.Type.POISON, entries.get(1).type());
    }

    @Test
    void silentPsychoStartsThroughAddonMixinBridge() {
        RecordingPsychoMode psychoMode = new RecordingPsychoMode(true);

        assertTrue(SilencerShopLoadout.startSilentPsycho(psychoMode));

        assertEquals(1, psychoMode.startRequests);
    }

    @Test
    void silentPsychoRefusesWhenAddonMixinBridgeIsAbsent() {
        assertFalse(SilencerShopLoadout.startSilentPsycho((SilentPsychoModeAccess) null));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }

    private static final class RecordingPsychoMode implements SilentPsychoModeAccess {
        private final boolean starts;
        private int startRequests;

        private RecordingPsychoMode(boolean starts) {
            this.starts = starts;
        }

        @Override
        public boolean strawcraft$startSilentPsycho() {
            startRequests++;
            return starts;
        }
    }
}
