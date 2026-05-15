package org.caecorthus.strawcraft;

import org.caecorthus.strawcraft.client.ShopEntryViewState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopEntryViewStateTest {
    @Test
    void shopEntryWithoutShopDataStaysActiveAndShowsOnlyPrice() {
        ShopEntryViewState state = ShopEntryViewState.withoutShop(300);

        assertTrue(state.active());
        assertEquals("300", state.priceText());
        assertTrue(state.status().isEmpty());
    }

    @Test
    void cooldownEntryIsInactiveAndShowsRemainingSeconds() {
        ShopEntryViewState state = ShopEntryViewState.fromSnapshot(new ShopEntryViewState.Snapshot(
                300,
                true,
                45,
                -1,
                0,
                true
        ));

        assertFalse(state.active());
        assertEquals("2s", state.status().orElseThrow().text());
        assertEquals(ShopEntryViewState.UNAVAILABLE_STATUS_COLOR, state.status().orElseThrow().color());
    }

    @Test
    void stockedEntryShowsRemainingStockAndTurnsRedWhenSoldOut() {
        ShopEntryViewState available = ShopEntryViewState.fromSnapshot(new ShopEntryViewState.Snapshot(
                120,
                false,
                0,
                3,
                2,
                true
        ));
        ShopEntryViewState soldOut = ShopEntryViewState.fromSnapshot(new ShopEntryViewState.Snapshot(
                120,
                false,
                0,
                3,
                0,
                false
        ));

        assertTrue(available.active());
        assertEquals("2", available.status().orElseThrow().text());
        assertEquals(ShopEntryViewState.DEFAULT_STATUS_COLOR, available.status().orElseThrow().color());
        assertFalse(soldOut.active());
        assertEquals("0", soldOut.status().orElseThrow().text());
        assertEquals(ShopEntryViewState.UNAVAILABLE_STATUS_COLOR, soldOut.status().orElseThrow().color());
    }
}
