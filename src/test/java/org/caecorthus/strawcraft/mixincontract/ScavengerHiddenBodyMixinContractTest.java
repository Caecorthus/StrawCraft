package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScavengerHiddenBodyMixinContractTest {
    @Test
    void officialWatheBodyRendererStillExposesCancelableRenderHook() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer",
                "render",
                "(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        ));

        MixinTargetBytecode.MixinClassInspection mixin = MixinTargetBytecode.inspectMixinClass(
                "org.caecorthus.strawcraft.mixin.client.PlayerBodyEntityRendererMixin",
                "dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer"
        );
        assertTrue(mixin.classPresent());
        assertTrue(mixin.targetsExpectedOwner());
        assertTrue(mixin.injections().stream().anyMatch(injection -> injection.targetsMethod("render")));
    }
}
