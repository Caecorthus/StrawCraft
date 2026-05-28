package org.caecorthus.strawcraft.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.caecorthus.strawcraft.client.MorphlingClientVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MorphlingCorpseAnglesMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            ))
    private void strawcraft$quietMorphlingCorpseAngles(
            T entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo callbackInfo
    ) {
        if (!(entity instanceof AbstractClientPlayerEntity player)
                || !MorphlingClientVisuals.isCorpseMode(player)
                || !(model instanceof PlayerEntityModel<?> playerModel)) {
            return;
        }

        playerModel.head.pitch = 0.0F;
        playerModel.head.yaw = 0.0F;
        playerModel.hat.copyTransform(playerModel.head);

        float ageInTicks = entity.age + tickDelta;
        float swingRollBase = MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        float swingPitchBase = MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        playerModel.rightArm.roll -= swingRollBase;
        playerModel.rightArm.pitch -= swingPitchBase;
        playerModel.leftArm.roll += swingRollBase;
        playerModel.leftArm.pitch += swingPitchBase;
        playerModel.leftSleeve.copyTransform(playerModel.leftArm);
        playerModel.rightSleeve.copyTransform(playerModel.rightArm);
    }
}
