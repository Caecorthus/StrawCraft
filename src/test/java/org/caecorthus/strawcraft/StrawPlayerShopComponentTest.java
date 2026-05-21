package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawPlayerShopComponentTest {
    @Test
    void entryStateTracksInitialCooldownPurchaseCooldownAndStock() {
        StrawShopEntry entry = new StrawShopEntry("p320", null, null, 300, ShopEntry.Type.WEAPON, 40, 20, 2);
        StrawPlayerShopState state = new StrawPlayerShopState();

        state.ensureEntry(entry, 100);

        assertTrue(state.isOnCooldown(entry.id(), 100));
        assertEquals(20, state.getRemainingCooldown(entry.id(), 100));
        assertEquals(2, state.getRemainingStock(entry.id()));
        assertTrue(state.isInStock(entry.id()));

        state.recordPurchase(entry, 120);

        assertTrue(state.isOnCooldown(entry.id(), 120));
        assertEquals(40, state.getRemainingCooldown(entry.id(), 120));
        assertEquals(1, state.getRemainingStock(entry.id()));

        state.recordPurchase(entry, 160);

        assertFalse(state.isInStock(entry.id()));
        assertEquals(0, state.getRemainingStock(entry.id()));
    }

    @Test
    void entryStateRoundTripsCooldownAndStockThroughNbt() {
        StrawShopEntry entry = new StrawShopEntry("p320", null, null, 300, ShopEntry.Type.WEAPON, 40, 20, 2);
        StrawPlayerShopState saved = new StrawPlayerShopState();
        saved.recordPurchase(entry, 100);

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        StrawPlayerShopState loaded = new StrawPlayerShopState();
        loaded.readFromNbt(nbt);

        assertTrue(loaded.isOnCooldown(entry.id(), 100));
        assertEquals(40, loaded.getRemainingCooldown(entry.id(), 100));
        assertEquals(1, loaded.getRemainingStock(entry.id()));
        assertTrue(loaded.canPurchase(entry, 140));
    }
}
