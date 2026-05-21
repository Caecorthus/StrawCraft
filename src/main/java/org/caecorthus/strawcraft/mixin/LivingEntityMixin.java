package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin {
    @Inject(method = "wathe$restrictJump", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$allowVanillaJump(CallbackInfo originalJumpCallback, CallbackInfo handlerCallback) {
        // Skip Wathe's injected jump gate so normal Minecraft jumping works during rounds.
        // 跳过 Wathe 注入的跳跃限制，让正式局里仍然使用 Minecraft 原版跳跃。
        handlerCallback.cancel();
    }
}
