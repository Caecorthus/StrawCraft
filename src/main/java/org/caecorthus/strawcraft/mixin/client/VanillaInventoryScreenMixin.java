package org.caecorthus.strawcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class, priority = 800)
public abstract class VanillaInventoryScreenMixin {
    @Inject(method = "wathe$replaceInventoryScreenWithLimitedInventoryScreen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$keepVanillaInventoryScreen(MinecraftClient instance, Screen screen, Operation<Void> original, CallbackInfo callbackInfo) {
        // Call Minecraft's original setScreen(screen) operation before Wathe can replace it with LimitedInventoryScreen.
        // 在 Wathe 替换成受限背包界面前，先调用 Minecraft 原本的 setScreen(screen) 操作。
        original.call(instance, screen);
        callbackInfo.cancel();
    }
}
