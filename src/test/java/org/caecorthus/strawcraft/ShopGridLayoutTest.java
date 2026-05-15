package org.caecorthus.strawcraft;

import org.caecorthus.strawcraft.client.ShopGridLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopGridLayoutTest {
    @Test
    void shopGridCapsAtFiveColumns() {
        assertEquals(5, ShopGridLayout.columnsFor(12, 260));
    }

    @Test
    void shopGridShrinksToFitNarrowScreens() {
        assertEquals(2, ShopGridLayout.columnsFor(7, 120));
        assertEquals(4, ShopGridLayout.rowsFor(7, 2));
    }

    @Test
    void shopGridPositionsSlotsFromPanelOrigin() {
        int columns = 3;

        assertEquals(66, ShopGridLayout.slotX(10, 4, columns));
        assertEquals(138, ShopGridLayout.slotY(30, 4, columns));
    }

    @Test
    void shopGridUsesWatheStyleSlotSpacing() {
        assertEquals(30, ShopGridLayout.SLOT_SIZE);
        assertEquals(38, ShopGridLayout.COLUMN_SPACING);
        assertEquals(60, ShopGridLayout.ROW_SPACING);
    }
}
