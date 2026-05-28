package org.caecorthus.strawcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.caecorthus.strawcraft.client.MorphlingClientVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class MorphlingPlayerEntityRendererMixin {
    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"),
            cancellable = true)
    private void strawcraft$useMorphlingDisguiseTexture(
            AbstractClientPlayerEntity player,
            CallbackInfoReturnable<Identifier> callbackInfo
    ) {
        MorphlingClientVisuals.disguiseSkinTextures(player)
                .map(SkinTextures::texture)
                .ifPresent(callbackInfo::setReturnValue);
    }

    @WrapOperation(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"
            )
    )
    private SkinTextures strawcraft$useMorphlingDisguiseArmTexture(
            AbstractClientPlayerEntity player,
            Operation<SkinTextures> original
    ) {
        return MorphlingClientVisuals.disguiseSkinTextures(player).orElseGet(() -> original.call(player));
    }

    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void strawcraft$useMorphlingCorpseTransforms(
            AbstractClientPlayerEntity player,
            MatrixStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta,
            float scale,
            CallbackInfo callbackInfo
    ) {
        if (!MorphlingClientVisuals.isCorpseMode(player)) {
            return;
        }
        // Match Spark's fake-corpse posture while keeping all authority in StrawCraft synced state.
        // 复用 Spark 的伪尸体姿态，但所有判定都来自 StrawCraft 同步状态。
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F - bodyYaw));
        matrices.translate(1.0F, 0.0F, 0.0F);
        matrices.translate(0.0F, 0.15F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        callbackInfo.cancel();
    }
}
