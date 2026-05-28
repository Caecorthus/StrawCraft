package org.caecorthus.strawcraft.mixin.client;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.caecorthus.strawcraft.ScavengerHiddenBodyClientVisibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerBodyEntityRenderer.class, priority = 900)
public abstract class PlayerBodyEntityRendererMixin {
    @Inject(method = "render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void strawcraft$hideScavengerBodiesFromOrdinaryPassengers(
            PlayerBodyEntity body,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo callbackInfo
    ) {
        if (!ScavengerHiddenBodyClientVisibility.shouldRender(body, MinecraftClient.getInstance())) {
            callbackInfo.cancel();
        }
    }
}
