package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import dev.doctor4t.wathe.util.StoreBuyPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class StrawCraftShopScreen extends Screen {
    private static final Text TITLE = Text.literal("Shop");
    private static final Text BACK_TO_INVENTORY = Text.literal("Inventory");
    private static final int PANEL_COLOR = 0xE0181820;
    private static final int PANEL_BORDER = 0xFFB8B8B8;
    private static final int PANEL_INNER_BORDER = 0xFF3B3B46;
    private static final int UNAVAILABLE_OVERLAY = 0xAA000000;
    private static final int BALANCE_COLOR = 0xFFFFE07A;

    private final List<ShopEntryButton> itemButtons = new ArrayList<>();
    private List<ShopEntry> entries = List.of();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public StrawCraftShopScreen() {
        super(TITLE);
    }

    public static boolean canOpen(MinecraftClient client) {
        if (client.player == null) {
            return false;
        }
        try {
            return !ShopUtils.getShopEntriesForPlayer(client.player).isEmpty();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    protected void init() {
        this.itemButtons.clear();
        this.entries = getEntries();

        int columns = ShopGridLayout.columnsFor(this.entries.size(), this.width - 40);
        int rows = ShopGridLayout.rowsFor(this.entries.size(), columns);
        this.panelWidth = Math.max(176, ShopGridLayout.panelWidth(columns));
        this.panelHeight = Math.max(130, ShopGridLayout.panelHeight(rows));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.addDrawableChild(ButtonWidget.builder(BACK_TO_INVENTORY, button -> openInventory())
                .dimensions(this.panelX + this.panelWidth - 78, this.panelY + this.panelHeight - 24, 66, 20)
                .tooltip(Tooltip.of(Text.literal("Back to vanilla inventory")))
                .build());

        for (int index = 0; index < this.entries.size(); index++) {
            ShopEntry entry = this.entries.get(index);
            ShopEntryButton button = new ShopEntryButton(
                    ShopGridLayout.slotX(this.panelX, index, columns),
                    ShopGridLayout.slotY(this.panelY, index, columns),
                    index,
                    entry
            );
            this.itemButtons.add(button);
            this.addDrawableChild(button);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        drawPanel(context);
        super.render(context, mouseX, mouseY, delta);
        drawHoveredTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private void drawPanel(DrawContext context) {
        context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, PANEL_COLOR);
        context.drawBorder(this.panelX, this.panelY, this.panelWidth, this.panelHeight, PANEL_BORDER);
        context.drawBorder(this.panelX + 2, this.panelY + 2, this.panelWidth - 4, this.panelHeight - 4, PANEL_INNER_BORDER);
        context.drawCenteredTextWithShadow(this.textRenderer, TITLE, this.width / 2, this.panelY + 10, 0xFFFFFF);

        ClientPlayerEntity player = getPlayer();
        if (player != null) {
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Balance: " + shop.getBalance()), this.panelX + 12, this.panelY + 22, BALANCE_COLOR);
        }

        if (this.entries.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No shop entries"), this.width / 2, this.panelY + 58, 0xA0A0A0);
        }
    }

    private void drawHoveredTooltip(DrawContext context, int mouseX, int mouseY) {
        for (ShopEntryButton button : this.itemButtons) {
            if (button.isHovered()) {
                context.drawItemTooltip(this.textRenderer, button.entry.displayStack(), mouseX, mouseY);
                return;
            }
        }
    }

    private void openInventory() {
        ClientPlayerEntity player = getPlayer();
        if (this.client != null && player != null) {
            this.client.setScreen(new InventoryScreen(player));
        }
    }

    private List<ShopEntry> getEntries() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) {
            return List.of();
        }
        try {
            return ShopUtils.getShopEntriesForPlayer(player);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private ClientPlayerEntity getPlayer() {
        return this.client == null ? null : this.client.player;
    }

    private static final class ShopEntryButton extends PressableWidget {
        private final int index;
        private final ShopEntry entry;

        private ShopEntryButton(int x, int y, int index, ShopEntry entry) {
            super(x, y, ShopGridLayout.SLOT_SIZE, ShopGridLayout.SLOT_SIZE, entry.displayStack().getName());
            this.index = index;
            this.entry = entry;
        }

        @Override
        public void onPress() {
            // Wathe revalidates the index server-side before completing the purchase.
            ClientPlayNetworking.send(new StoreBuyPayload(this.index));
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            PlayerShopComponent shop = shopComponent();
            ShopEntryViewState state = viewState(shop);
            this.active = state.active();

            int x = getX();
            int y = getY();
            int background = this.isHovered() ? 0xFF4A4A56 : 0xFF30303A;
            context.fill(x, y, x + this.width, y + this.height, background);
            context.drawBorder(x, y, this.width, this.height, this.active ? 0xFFE0E0E0 : 0xFF777777);
            context.drawItem(this.entry.displayStack(), x + 7, y + 5);

            if (!this.active) {
                context.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, UNAVAILABLE_OVERLAY);
            }

            drawStatus(context, state);
            drawPrice(context, state);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }

        private void drawStatus(DrawContext context, ShopEntryViewState state) {
            state.status().ifPresent(status -> {
                int textX = getX() + this.width - MinecraftClient.getInstance().textRenderer.getWidth(status.text()) - 3;
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, status.text(), textX, getY() + 2, status.color());
            });
        }

        private void drawPrice(DrawContext context, ShopEntryViewState state) {
            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(state.priceText());
            int textX = getX() + (this.width - textWidth) / 2;
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, state.priceText(), textX, getY() + this.height - 9, BALANCE_COLOR);
        }

        private PlayerShopComponent shopComponent() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            return player == null ? null : PlayerShopComponent.KEY.get(player);
        }

        private ShopEntryViewState viewState(PlayerShopComponent shop) {
            if (shop == null) {
                return ShopEntryViewState.withoutShop(this.entry.price());
            }
            return ShopEntryViewState.fromSnapshot(new ShopEntryViewState.Snapshot(
                    this.entry.price(),
                    shop.isOnCooldown(this.entry.id()),
                    shop.getRemainingCooldown(this.entry.id()),
                    shop.getMaxStock(this.entry.id()),
                    shop.getRemainingStock(this.entry.id()),
                    shop.isInStock(this.entry.id())
            ));
        }
    }
}
