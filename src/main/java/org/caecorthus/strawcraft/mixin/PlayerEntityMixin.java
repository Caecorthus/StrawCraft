package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import org.caecorthus.strawcraft.WatheDeathReasonTracker;
import org.caecorthus.strawcraft.map.StrawPlayerEnhancementAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEntity.class, priority = 900)
public abstract class PlayerEntityMixin {
    @Inject(method = "wathe$overrideMovementSpeed", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$keepVanillaMovementSpeed(float original, CallbackInfoReturnable<Float> handlerCallback) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        // Start from Minecraft's own speed, then apply optional map multipliers.
        // 先保留 Minecraft 自己算出的速度，再叠加可选地图倍率。
        handlerCallback.setReturnValue(StrawPlayerEnhancementAdapter.movementSpeed(player, original));
    }

    @Inject(method = "wathe$limitSprint", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$disableWatheStaminaLimit(CallbackInfo originalTickCallback, CallbackInfo handlerCallback) {
        // Skip Wathe stamina exhaustion and mood-based sprint blocking.
        // 跳过 Wathe 的体力耗尽和心情疾跑限制。
        handlerCallback.cancel();
    }

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void strawcraft$rememberTaczBulletDeathReason(DamageSource source, float amount, CallbackInfo callback) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        source.getTypeRegistryEntry().getKey()
                .map(RegistryKey::getValue)
                .flatMap(WatheDeathReasonTracker::watheReasonForDamageType)
                .ifPresent(deathReason -> {
                    if (strawcraft$isGoodPlayerShotByGoodPlayer(player, source)) {
                        WatheDeathReasonTracker.rememberShotInnocentDeath(player.getUuid());
                        return;
                    }
                    WatheDeathReasonTracker.rememberDeathAttribution(player.getUuid(), deathReason, source);
                });
    }

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void strawcraft$clearNonLethalTaczBulletDeathReason(DamageSource source, float amount, CallbackInfo callback) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.isAlive()) {
            WatheDeathReasonTracker.clearDeathReason(player.getUuid());
        }
    }

    private static boolean strawcraft$isGoodPlayerShotByGoodPlayer(PlayerEntity victim, DamageSource source) {
        if (victim.getWorld().isClient() || !(source.getAttacker() instanceof PlayerEntity attacker)) {
            return false;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        return game.isRunning() && game.isInnocent(victim) && game.isInnocent(attacker);
    }
}
