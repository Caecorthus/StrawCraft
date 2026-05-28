package org.caecorthus.strawcraft.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.caecorthus.strawcraft.client.MorphlingClientVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class MorphlingEntityRenderDispatcherMixin {
    @Shadow
    private Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity>> modelRenderers;

    @SuppressWarnings("unchecked")
    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void strawcraft$useMorphlingDisguiseModel(
            T entity,
            CallbackInfoReturnable<EntityRenderer<? super T>> callbackInfo
    ) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        MorphlingClientVisuals.disguiseModel(player)
                .map(modelRenderers::get)
                .ifPresent(renderer -> callbackInfo.setReturnValue((EntityRenderer<? super T>) renderer));
    }
}
