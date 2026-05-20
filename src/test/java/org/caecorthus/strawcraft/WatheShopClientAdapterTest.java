package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.caecorthus.strawcraft.client.ShopEntryViewState;
import org.caecorthus.strawcraft.client.WatheShopClientAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatheShopClientAdapterTest {
    @Test
    void createsPureSnapshotFromWatheEntriesAndShopState() {
        ShopEntry revolver = new ShopEntry.Builder("revolver", null, 300, ShopEntry.Type.WEAPON).build();
        ShopEntry knife = new ShopEntry.Builder("knife", null, 100, ShopEntry.Type.WEAPON).build();

        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                List.of(revolver, knife),
                new FakeShopState()
        );

        assertEquals(OptionalInt.of(450), snapshot.balance());
        assertEquals(2, snapshot.entries().size());
        assertSame(revolver, snapshot.entries().get(0));
        assertSame(knife, snapshot.entries().get(1));

        ShopEntryViewState revolverState = snapshot.entryStates().get(0);
        assertFalse(revolverState.active());
        assertEquals("15s", revolverState.cooldownStatus().orElseThrow().text());
        assertEquals("1", revolverState.stockStatus().orElseThrow().text());

        ShopEntryViewState knifeState = snapshot.entryStates().get(1);
        assertTrue(knifeState.active());
        assertTrue(knifeState.status().isEmpty());
    }

    @Test
    void missingShopStateStillKeepsEntriesVisibleWithPrices() {
        ShopEntry revolver = new ShopEntry.Builder("revolver", null, 300, ShopEntry.Type.WEAPON).build();

        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(List.of(revolver), null);

        assertTrue(snapshot.balance().isEmpty());
        assertEquals(1, snapshot.entries().size());
        assertTrue(snapshot.entryStates().get(0).active());
        assertEquals("300", snapshot.entryStates().get(0).priceText());
    }

    @Test
    void entryKeysIgnoreShopStateAndRepresentCustomData() {
        ShopEntry p320 = new ShopEntry.Builder("p320", null, 300, ShopEntry.Type.WEAPON).build();
        ShopEntry p320OnCooldown = new ShopEntry.Builder("p320", null, 300, ShopEntry.Type.WEAPON).build();

        WatheShopClientAdapter.ShopSnapshot available = WatheShopClientAdapter.snapshotFrom(
                List.of(p320),
                new FakeShopState()
        );
        WatheShopClientAdapter.ShopSnapshot cooldown = WatheShopClientAdapter.snapshotFrom(
                List.of(p320OnCooldown),
                new FakeShopState()
        );

        assertEquals(available.entryKeys(), cooldown.entryKeys());
        assertFalse(stackKey("GunId=tacz:p320").equals(stackKey("GunId=tacz:rhino357")));
    }

    private static final class FakeShopState implements WatheShopClientAdapter.ShopState {
        @Override
        public OptionalInt balance() {
            return OptionalInt.of(450);
        }

        @Override
        public boolean isOnCooldown(String entryId) {
            return "revolver".equals(entryId);
        }

        @Override
        public int remainingCooldownTicks(String entryId) {
            return "revolver".equals(entryId) ? 300 : 0;
        }

        @Override
        public int maxStock(String entryId) {
            return "revolver".equals(entryId) ? 2 : -1;
        }

        @Override
        public int remainingStock(String entryId) {
            return "revolver".equals(entryId) ? 1 : -1;
        }

        @Override
        public boolean isInStock(String entryId) {
            return true;
        }
    }

    private static WatheShopClientAdapter.StackKey stackKey(String customData) {
        return new WatheShopClientAdapter.StackKey("tacz:modern_kinetic_gun", 1, "Modern Kinetic Gun", customData);
    }
}
