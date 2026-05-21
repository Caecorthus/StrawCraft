package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import org.caecorthus.strawcraft.WatheDeathReasonTracker;
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
        // 直接返回 Minecraft 已经算好的速度，保留原版行走、疾跑和效果计算。
        handlerCallback.setReturnValue(original);
    }

    @Inject(method = "wathe$limitSprint", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$disableWatheStaminaLimit(CallbackInfo originalTickCallback, CallbackInfo handlerCallback) {
        // Skip Wathe stamina exhaustion and mood-based sprint blocking.
        // 跳过 Wathe 的体力耗尽和心情疾跑限制。
        handlerCallback.cancel();
    }

    @Inject(method = "wathe$cancelApplyDamage", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$allowVanillaDamage(DamageSource source, float amount, CallbackInfo originalDamageCallback, CallbackInfo handlerCallback) {
        // Let PlayerEntity.applyDamage subtract hearts normally during a Wathe round.
        // 让 Wathe 局内的 PlayerEntity.applyDamage 仍然按原版方式扣红心。
        handlerCallback.cancel();
    }

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void strawcraft$rememberTaczBulletDeathReason(DamageSource source, float amount, CallbackInfo callback) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        source.getTypeRegistryEntry().getKey()
                .map(RegistryKey::getValue)
                .flatMap(WatheDeathReasonTracker::watheReasonForDamageType)
                .ifPresent(deathReason -> WatheDeathReasonTracker.rememberDeathAttribution(player.getUuid(), deathReason, source));
    }

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void strawcraft$clearNonLethalTaczBulletDeathReason(DamageSource source, float amount, CallbackInfo callback) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.isAlive()) {
            WatheDeathReasonTracker.clearDeathReason(player.getUuid());
        }
    }
}
