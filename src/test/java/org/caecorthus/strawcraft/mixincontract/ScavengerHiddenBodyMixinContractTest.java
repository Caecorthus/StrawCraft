package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScavengerHiddenBodyMixinContractTest {
    private static final String OFFICIAL_KILL_PLAYER_DESCRIPTOR =
            "(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;"
                    + "Lnet/minecraft/util/Identifier;)V";
    private static final String WORLD_SPAWN_ENTITY_TARGET =
            "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z";

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

    @Test
    void officialWatheKillPlayerStillSpawnsBodyThroughRedirectedWorldHook() {
        assertTrue(MixinTargetBytecode.hasMethod(
                "dev.doctor4t.wathe.game.GameFunctions",
                "killPlayer",
                OFFICIAL_KILL_PLAYER_DESCRIPTOR
        ));
        assertTrue(MixinTargetBytecode.hasMethodInvocation(
                "dev.doctor4t.wathe.game.GameFunctions",
                "killPlayer",
                OFFICIAL_KILL_PLAYER_DESCRIPTOR,
                "net/minecraft/world/World",
                "spawnEntity",
                "(Lnet/minecraft/entity/Entity;)Z"
        ), "Official Wathe GameFunctions.killPlayer must still call World.spawnEntity(Entity) for body spawning.");

        MixinTargetBytecode.MixinClassInspection mixin = MixinTargetBytecode.inspectMixinClass(
                "org.caecorthus.strawcraft.mixin.GameFunctionsMixin",
                "dev.doctor4t.wathe.game.GameFunctions"
        );
        assertTrue(mixin.classPresent());
        assertTrue(mixin.targetsExpectedOwner());
        assertTrue(mixin.injections().stream().anyMatch(injection ->
                "@Redirect".equals(injection.annotationName())
                        && injection.targetsMethod("killPlayer")
                        && injection.targetsAt(WORLD_SPAWN_ENTITY_TARGET)));
    }
}
