package org.caecorthus.strawcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.LivingEntity;
import org.caecorthus.strawcraft.client.MorphlingClientVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ElytraFeatureRenderer.class)
public abstract class MorphlingElytraFeatureRendererMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"
            )
    )
    private SkinTextures strawcraft$useMorphlingDisguiseElytraTexture(
            AbstractClientPlayerEntity player,
            Operation<SkinTextures> original
    ) {
        if (!(player instanceof LivingEntity)) {
            return original.call(player);
        }
        return MorphlingClientVisuals.disguiseSkinTextures(player).orElseGet(() -> original.call(player));
    }
}
