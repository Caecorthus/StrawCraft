package org.caecorthus.strawcraft.client;

public final class ShopGridLayout {
    public static final int SLOT_SIZE = 30;
    public static final int COLUMN_SPACING = 38;
    public static final int ROW_SPACING = 60;
    public static final int TOP_PADDING = 48;
    public static final int BOTTOM_PADDING = 32;
    public static final int SIDE_PADDING = 18;

    private static final int MIN_COLUMNS = 1;
    private static final int MAX_COLUMNS = 5;

    private ShopGridLayout() {
    }

    public static int columnsFor(int entryCount, int availableWidth) {
        if (entryCount <= 0) {
            return 0;
        }

        int usableWidth = Math.max(0, availableWidth - SIDE_PADDING * 2);
        int columnsByWidth = MIN_COLUMNS;
        if (usableWidth > SLOT_SIZE) {
            columnsByWidth += (usableWidth - SLOT_SIZE) / COLUMN_SPACING;
        }
        return Math.min(Math.min(entryCount, MAX_COLUMNS), columnsByWidth);
    }

    public static int rowsFor(int entryCount, int columns) {
        if (entryCount <= 0 || columns <= 0) {
            return 0;
        }
        return Math.ceilDiv(entryCount, columns);
    }

    public static int panelWidth(int columns) {
        if (columns <= 0) {
            return SIDE_PADDING * 2;
        }
        return SIDE_PADDING * 2 + SLOT_SIZE + (columns - 1) * COLUMN_SPACING;
    }

    public static int panelHeight(int rows) {
        if (rows <= 0) {
            return TOP_PADDING + BOTTOM_PADDING;
        }
        return TOP_PADDING + BOTTOM_PADDING + SLOT_SIZE + (rows - 1) * ROW_SPACING;
    }

    public static int slotX(int panelX, int index, int columns) {
        return panelX + SIDE_PADDING + column(index, columns) * COLUMN_SPACING;
    }

    public static int slotY(int panelY, int index, int columns) {
        return panelY + TOP_PADDING + row(index, columns) * ROW_SPACING;
    }

    static int column(int index, int columns) {
        if (columns <= 0) {
            return 0;
        }
        return Math.floorMod(index, columns);
    }

    static int row(int index, int columns) {
        if (columns <= 0) {
            return 0;
        }
        return Math.max(0, index / columns);
    }
}
