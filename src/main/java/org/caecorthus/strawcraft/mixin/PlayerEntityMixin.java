package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEntity.class, priority = 900)
public abstract class PlayerEntityMixin {
    @Inject(method = "wathe$overrideMovementSpeed", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$keepVanillaMovementSpeed(float original, CallbackInfoReturnable<Float> handlerCallback) {
        // Return the speed Minecraft already calculated, preserving vanilla walk/sprint/effect math.
        handlerCallback.setReturnValue(original);
    }

    @Inject(method = "wathe$limitSprint", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$disableWatheStaminaLimit(CallbackInfo originalTickCallback, CallbackInfo handlerCallback) {
        // Skip Wathe stamina exhaustion and mood-based sprint blocking.
        handlerCallback.cancel();
    }

    @Inject(method = "wathe$cancelApplyDamage", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$allowVanillaDamage(DamageSource source, float amount, CallbackInfo originalDamageCallback, CallbackInfo handlerCallback) {
        // Let PlayerEntity.applyDamage subtract hearts normally during a Wathe round.
        handlerCallback.cancel();
    }
}
