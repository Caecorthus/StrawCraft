package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class WatheShopSlotRenderer {
    private static final int ITEM_OFFSET = 7;
    private static final int ITEM_SIZE = 16;
    private static final int UNAVAILABLE_ITEM_OVERLAY = 0xAA000000;
    private static final int PRICE_LABEL_BACKGROUND = 0xC0000000;
    private static final int PRICE_LABEL_BORDER = 0xFF5F5F68;
    private static final int PRICE_LABEL_COLOR = 0xFFFFE07A;
    private static final int PRICE_LABEL_PADDING_X = 3;
    private static final int PRICE_LABEL_PADDING_Y = 1;
    private static final int PRICE_LABEL_GAP = 3;

    private WatheShopSlotRenderer() {
    }

    public static void render(DrawContext context, TextRenderer textRenderer, ShopEntry entry, ShopEntryViewState state, int x, int y) {
        // Reference Wathe's runtime slot sprites through its public shop entry type; do not copy assets into StrawCraft.
        // 通过 Wathe 公开的商店条目类型引用运行时槽位贴图，不把资源复制进 StrawCraft。
        context.drawGuiTexture(entry.type().getTexture(), x, y, ShopGridLayout.SLOT_SIZE, ShopGridLayout.SLOT_SIZE);
        context.drawItem(entry.stack(), x + ITEM_OFFSET, y + ITEM_OFFSET);

        if (!state.active()) {
            context.fill(
                    x + ITEM_OFFSET,
                    y + ITEM_OFFSET,
                    x + ITEM_OFFSET + ITEM_SIZE,
                    y + ITEM_OFFSET + ITEM_SIZE,
                    UNAVAILABLE_ITEM_OVERLAY
            );
        }

        drawCooldown(context, textRenderer, state, x, y);
        drawStock(context, textRenderer, state, x, y);
        drawPriceLabel(context, textRenderer, state.priceText(), x, y);
    }

    private static void drawCooldown(DrawContext context, TextRenderer textRenderer, ShopEntryViewState state, int x, int y) {
        state.cooldownStatus().ifPresent(status -> {
            int textX = x + (ShopGridLayout.SLOT_SIZE - textRenderer.getWidth(status.text())) / 2;
            context.drawTextWithShadow(textRenderer, status.text(), textX, y + 11, status.color());
        });
    }

    private static void drawStock(DrawContext context, TextRenderer textRenderer, ShopEntryViewState state, int x, int y) {
        state.stockStatus().ifPresent(status -> {
            int textX = x + 23 - textRenderer.getWidth(status.text());
            context.drawTextWithShadow(textRenderer, status.text(), textX, y + 15, status.color());
        });
    }

    private static void drawPriceLabel(DrawContext context, TextRenderer textRenderer, String priceText, int x, int y) {
        Text price = Text.literal(priceText);
        int textWidth = textRenderer.getWidth(price);
        int labelWidth = textWidth + PRICE_LABEL_PADDING_X * 2;
        int labelHeight = textRenderer.fontHeight + PRICE_LABEL_PADDING_Y * 2;
        int labelX = x + (ShopGridLayout.SLOT_SIZE - labelWidth) / 2;
        int labelY = y - labelHeight - PRICE_LABEL_GAP;

        context.fill(labelX, labelY, labelX + labelWidth, labelY + labelHeight, PRICE_LABEL_BACKGROUND);
        context.drawBorder(labelX, labelY, labelWidth, labelHeight, PRICE_LABEL_BORDER);
        context.drawTextWithShadow(
                textRenderer,
                price,
                labelX + PRICE_LABEL_PADDING_X,
                labelY + PRICE_LABEL_PADDING_Y,
                PRICE_LABEL_COLOR
        );
    }
}
