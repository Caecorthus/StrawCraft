package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.caecorthus.strawcraft.SpiritualistProjectionRuntime;
import org.caecorthus.strawcraft.map.StrawPlayerEnhancementAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void strawcraft$applyMapJumpRule(CallbackInfo callback) {
        if ((Object) this instanceof PlayerEntity player && !StrawPlayerEnhancementAdapter.allowsJump(player)) {
            // Only the boolean jump gate is runtime-enabled; TMM stamina cost is modeled but not consumed.
            // 运行时只启用是否允许跳跃；TMM 的体力消耗字段已建模，但不在没有其体力组件时扣除。
            callback.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void strawcraft$forceSpiritualistReturnOnDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            SpiritualistProjectionRuntime.forceReturnAfterDamage(player, amount);
        }
    }
}
