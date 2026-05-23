package org.caecorthus.strawcraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
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
    private static final int BALANCE_COLOR = 0xFFFFE07A;
    private static final int SNAPSHOT_REFRESH_INTERVAL_TICKS = 10;

    private final WatheShopClientAdapter shopAdapter;
    private final List<ShopEntryButton> itemButtons = new ArrayList<>();
    private WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.ShopSnapshot.empty();
    private int ticksUntilSnapshotRefresh;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public StrawCraftShopScreen() {
        this(new WatheShopClientAdapter());
    }

    StrawCraftShopScreen(WatheShopClientAdapter shopAdapter) {
        super(TITLE);
        this.shopAdapter = shopAdapter;
    }

    public static boolean canOpen(MinecraftClient client) {
        return new WatheShopClientAdapter().canOpen(client);
    }

    @Override
    protected void init() {
        this.itemButtons.clear();
        this.snapshot = this.shopAdapter.snapshot(this.client);
        this.rebuildWidgetsFromSnapshot();
    }

    @Override
    public void tick() {
        if (this.client == null || --this.ticksUntilSnapshotRefresh > 0) {
            return;
        }
        this.ticksUntilSnapshotRefresh = SNAPSHOT_REFRESH_INTERVAL_TICKS;

        WatheShopClientAdapter.ShopSnapshot nextSnapshot = this.shopAdapter.snapshot(this.client);
        if (this.snapshot.entryKeys().equals(nextSnapshot.entryKeys())) {
            // Keep the existing widgets when only Wathe shop state changes, preserving
            // hover/focus while cooldowns, stock, and balance refresh in place.
            // 如果只是商店状态变化，就保留现有按钮；
            // 这样冷却、库存和余额刷新时不会打断悬停或焦点状态。
            updateSnapshotState(nextSnapshot);
        } else {
            this.snapshot = nextSnapshot;
            rebuildWidgetsFromSnapshot();
        }
    }

    private void rebuildWidgetsFromSnapshot() {
        this.clearChildren();
        this.itemButtons.clear();

        int columns = ShopGridLayout.columnsFor(this.snapshot.entries().size(), this.width - 40);
        int rows = ShopGridLayout.rowsFor(this.snapshot.entries().size(), columns);
        this.panelWidth = Math.max(176, ShopGridLayout.panelWidth(columns));
        this.panelHeight = Math.max(130, ShopGridLayout.panelHeight(rows));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.addDrawableChild(ButtonWidget.builder(BACK_TO_INVENTORY, button -> openInventory())
                .dimensions(this.panelX + this.panelWidth - 78, this.panelY + this.panelHeight - 24, 66, 20)
                .tooltip(Tooltip.of(Text.literal("Back to vanilla inventory")))
                .build());

        for (int index = 0; index < this.snapshot.entries().size(); index++) {
            // Button order is the visible catalog order; the adapter maps it to Wathe's purchase order.
            // 按钮顺序是可见目录顺序；适配层会把它映射回 Wathe 的购买顺序。
            ShopEntryViewState state = this.snapshot.entryStates().get(index);
            ShopEntryButton button = new ShopEntryButton(
                    ShopGridLayout.slotX(this.panelX, index, columns),
                    ShopGridLayout.slotY(this.panelY, index, columns),
                    index,
                    state,
                    this.shopAdapter
            );
            this.itemButtons.add(button);
            this.addDrawableChild(button);
        }
    }

    private void updateSnapshotState(WatheShopClientAdapter.ShopSnapshot nextSnapshot) {
        this.snapshot = nextSnapshot;
        for (int index = 0; index < this.itemButtons.size() && index < nextSnapshot.entryStates().size(); index++) {
            this.itemButtons.get(index).setState(nextSnapshot.entryStates().get(index));
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

        this.snapshot.balance().ifPresent(balance -> context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Balance: " + balance),
                this.panelX + 12,
                this.panelY + 22,
                BALANCE_COLOR
        ));

        if (this.snapshot.entries().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No shop entries"), this.width / 2, this.panelY + 58, 0xA0A0A0);
        }
    }

    private void drawHoveredTooltip(DrawContext context, int mouseX, int mouseY) {
        for (ShopEntryButton button : this.itemButtons) {
            if (button.isHovered()) {
                ItemStack displayStack = button.state.displayStack();
                if (displayStack != null) {
                    context.drawItemTooltip(this.textRenderer, displayStack, mouseX, mouseY);
                }
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

    private ClientPlayerEntity getPlayer() {
        return this.client == null ? null : this.client.player;
    }

    private static final class ShopEntryButton extends PressableWidget {
        private final int visibleIndex;
        private ShopEntryViewState state;
        private final WatheShopClientAdapter shopAdapter;

        private ShopEntryButton(int x, int y, int visibleIndex, ShopEntryViewState state, WatheShopClientAdapter shopAdapter) {
            super(x, y, ShopGridLayout.SLOT_SIZE, ShopGridLayout.SLOT_SIZE, displayNameFor(state));
            this.visibleIndex = visibleIndex;
            this.state = state;
            this.shopAdapter = shopAdapter;
        }

        private void setState(ShopEntryViewState state) {
            this.state = state;
        }

        @Override
        public void onPress() {
            this.shopAdapter.buy(this.visibleIndex);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            this.active = this.state.active();

            int x = getX();
            int y = getY();
            WatheShopSlotRenderer.render(context, MinecraftClient.getInstance().textRenderer, this.state, x, y);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }

        private static Text displayNameFor(ShopEntryViewState state) {
            ItemStack displayStack = state.displayStack();
            return displayStack == null ? Text.empty() : displayStack.getName();
        }
    }
}
