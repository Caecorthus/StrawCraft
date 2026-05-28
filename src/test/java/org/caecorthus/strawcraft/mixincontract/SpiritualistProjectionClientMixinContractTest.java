package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritualistProjectionClientMixinContractTest {
    @Test
    void minecraftClientStillExposesCameraEntitySetterForProjectionView() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.MinecraftClient",
                "setCameraEntity",
                "(Lnet/minecraft/entity/Entity;)V"
        ));
    }

    @Test
    void entityStillExposesLookStateForProjectionMarkerSync() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.entity.Entity",
                "getYaw",
                "()F"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.entity.Entity",
                "getPitch",
                "()F"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.entity.Entity",
                "setYaw",
                "(F)V"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.entity.Entity",
                "setPitch",
                "(F)V"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.entity.Entity",
                "setHeadYaw",
                "(F)V"
        ));
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.entity.Entity",
                "setBodyYaw",
                "(F)V"
        ));
    }

    @Test
    void keyboardInputStillExposesTickHookForProjectionSuppression() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "net.minecraft.client.input.KeyboardInput",
                "tick",
                "(ZF)V"
        ));
    }
}
