package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MorphlingVisualMixinContractTest {
    @Test
    void minecraftPlayerRendererStillExposesMorphlingTextureArmAndCorpseHooks() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.render.entity.PlayerEntityRenderer",
                "getTexture",
                "(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.render.entity.PlayerEntityRenderer",
                "renderArm",
                "(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I"
                        + "Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/model/ModelPart;"
                        + "Lnet/minecraft/client/model/ModelPart;)V"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.render.entity.PlayerEntityRenderer",
                "setupTransforms",
                "(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V"
        ));
    }

    @Test
    void featureAndHudRenderersStillExposeMorphlingWrapTargets() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.render.entity.EntityRenderDispatcher",
                "getRenderer",
                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.render.entity.feature.CapeFeatureRenderer",
                "render",
                "(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I"
                        + "Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.render.entity.feature.ElytraFeatureRenderer",
                "render",
                "(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I"
                        + "Lnet/minecraft/entity/LivingEntity;FFFFFF)V"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "dev.doctor4t.wathe.client.gui.RoleNameRenderer",
                "renderHud",
                "(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/network/ClientPlayerEntity;"
                        + "Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
        ));
    }
}
