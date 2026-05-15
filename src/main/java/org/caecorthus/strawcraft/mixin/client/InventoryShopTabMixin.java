package org.caecorthus.strawcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.client.StrawCraftShopScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryShopTabMixin extends HandledScreen<PlayerScreenHandler> {
    private static final int SHOP_TAB_SIZE = 24;
    private static final int SHOP_TAB_GAP = 4;

    private InventoryShopTabMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void strawcraft$addShopTab(CallbackInfo callbackInfo) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StrawCraftShopScreen.canOpen(client)) {
            return;
        }

        int tabX = this.x - SHOP_TAB_SIZE - SHOP_TAB_GAP;
        int tabY = this.y + SHOP_TAB_SIZE + SHOP_TAB_GAP;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("S"), button -> client.setScreen(new StrawCraftShopScreen()))
                .dimensions(tabX, tabY, SHOP_TAB_SIZE, SHOP_TAB_SIZE)
                .tooltip(Tooltip.of(Text.literal("Open shop")))
                .build());
    }
}
