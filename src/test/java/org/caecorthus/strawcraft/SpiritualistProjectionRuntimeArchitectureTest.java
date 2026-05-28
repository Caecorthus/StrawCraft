package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritualistProjectionRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/SpiritualistProjectionRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/SpiritualistProjectionPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path CLIENT_VIEW =
            Path.of("src/main/java/org/caecorthus/strawcraft/client/SpiritualistProjectionClientView.java");
    private static final Path ROLE_CATALOG = Path.of("src/main/java/org/caecorthus/strawcraft/NoellesRoleCatalog.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");
    private static final Path LIVING_MIXIN = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/LivingEntityMixin.java");
    private static final Path SOUND_MIXIN = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/ServerCommonNetworkHandlerMixin.java");
    private static final Path INPUT_MIXIN =
            Path.of("src/main/java/org/caecorthus/strawcraft/mixin/client/SpiritualistKeyboardInputMixin.java");
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/strawcraft.mixins.json");

    @Test
    void payloadCarriesOnlyEmptyProjectionIntent() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record SpiritualistProjectionPayload()"));
        assertTrue(payload.contains("PacketCodec.unit(new SpiritualistProjectionPayload())"));
        assertFalse(payload.contains("UUID"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
        assertFalse(payload.contains("Role"));
    }

    @Test
    void runtimeOwnsProjectionValidationStateCooldownAndFeedback() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(SpiritualistProjectionPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(SpiritualistProjectionPayload.ID"));
        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register"));
        assertTrue(runtime.contains("ServerPlayConnectionEvents.DISCONNECT.register"));
        assertTrue(runtime.contains("forceReturnOnDisconnect(handler.getPlayer())"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(player)"));
        assertTrue(runtime.contains("SpiritualistProjectionPolicy.forceReturn(roleState, world.getTime())"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesSpiritualistProjection"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(spiritualist)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(SpiritualistProjectionPolicy.ABILITY_ID"));
        assertTrue(runtime.contains("roleState.isTaotieSwallowed()"));
        assertTrue(runtime.contains("SpiritualistProjectionPolicy.startProjecting"));
        assertTrue(runtime.contains("SpiritualistProjectionPolicy.returnToBody"));
        assertTrue(runtime.contains("SpiritualistProjectionPolicy.forceReturn"));
        assertTrue(runtime.contains("message.strawcraft.spiritualist.projecting"));
        assertTrue(runtime.contains("message.strawcraft.spiritualist.returned"));
        assertTrue(runtime.indexOf("SpiritualistProjectionPolicy.validateToggle")
                < runtime.indexOf("switch (result)"));
    }

    @Test
    void damageAndSoundMixinsDelegateToProjectionRuntime() throws IOException {
        String livingMixin = Files.readString(LIVING_MIXIN, StandardCharsets.UTF_8);
        String soundMixin = Files.readString(SOUND_MIXIN, StandardCharsets.UTF_8);
        String mixinConfig = Files.readString(MIXIN_CONFIG, StandardCharsets.UTF_8);

        assertTrue(livingMixin.contains("SpiritualistProjectionRuntime.forceReturnAfterDamage"));
        assertTrue(livingMixin.contains("method = \"damage\""));
        assertTrue(soundMixin.contains("PlaySoundS2CPacket"));
        assertTrue(soundMixin.contains("PlaySoundFromEntityS2CPacket"));
        assertTrue(soundMixin.contains("SpiritualistProjectionRuntime.shouldSuppressSoundPacket"));
        assertTrue(mixinConfig.contains("ServerCommonNetworkHandlerMixin"));
    }

    @Test
    void commonAndClientRegistrationUseIntentOnlyKeybind() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("SpiritualistProjectionRuntime.register()"));
        assertTrue(client.contains("\"key.strawcraft.spiritualist_project\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickSpiritualistProjection)"));
        assertTrue(client.contains("SpiritualistProjectionClientView.tick(client)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new SpiritualistProjectionPayload())"));
        assertFalse(client.contains("new SpiritualistProjectionPayload(target"));
    }

    @Test
    void playableProjectionUsesClientOnlyMarkerCameraAndSuppressesRealMovement() throws IOException {
        String catalog = Files.readString(ROLE_CATALOG, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String clientView = Files.readString(CLIENT_VIEW, StandardCharsets.UTF_8);
        String inputMixin = Files.readString(INPUT_MIXIN, StandardCharsets.UTF_8);
        String mixinConfig = Files.readString(MIXIN_CONFIG, StandardCharsets.UTF_8);

        assertTrue(catalog.contains("selectableGood(\"spiritualist\")"));
        assertTrue(client.contains("SpiritualistProjectionClientView.tick(client)"));
        assertTrue(clientView.contains("new MarkerEntity(EntityType.MARKER, client.world)"));
        assertTrue(clientView.contains("client.setCameraEntity(marker)"));
        assertTrue(clientView.contains("client.setCameraEntity(client.player)"));
        assertTrue(clientView.contains("NoellesRoleStateComponent.KEY.get(client.player).spiritualistProjection()"));
        assertTrue(clientView.contains("client.options.forwardKey.isPressed()"));
        assertTrue(clientView.contains("MAX_SPEED_PER_TICK"));
        assertTrue(clientView.contains("cleanup(client)"));
        assertTrue(clientView.contains("syncMarkerLook(client)"));
        assertTrue(clientView.contains("float yaw = client.player.getYaw()"));
        assertTrue(clientView.contains("float pitch = client.player.getPitch()"));
        assertTrue(clientView.contains("marker.setYaw(yaw)"));
        assertTrue(clientView.contains("marker.setPitch(pitch)"));
        assertTrue(clientView.contains("marker.prevYaw = client.player.prevYaw"));
        assertTrue(clientView.contains("marker.prevPitch = client.player.prevPitch"));
        assertTrue(clientView.contains("marker.setHeadYaw(yaw)"));
        assertTrue(clientView.contains("marker.setBodyYaw(yaw)"));
        assertTrue(clientView.indexOf("syncMarkerLook(client)")
                < clientView.indexOf("Vec3d forward = Vec3d.fromPolar(0.0F, marker.getYaw())"));
        assertTrue(clientView.contains("pendingCameraRestore"));
        assertTrue(clientView.contains("restorePendingCamera(client)"));
        assertTrue(clientView.indexOf("restorePendingCamera(client)")
                < clientView.indexOf("if (client.player == null || client.world == null)"));
        assertTrue(clientView.contains("if (client.getCameraEntity() == marker)"));
        assertTrue(clientView.contains("pendingCameraRestore = true"));
        assertTrue(clientView.contains("client.setCameraEntity(client.player)"));
        assertTrue(inputMixin.contains("@Mixin(KeyboardInput.class)"));
        assertTrue(inputMixin.contains("method = \"tick(ZF)V\""));
        assertTrue(inputMixin.contains("SpiritualistProjectionClientView.isProjecting()"));
        assertTrue(inputMixin.contains("movementForward = 0.0F"));
        assertTrue(inputMixin.contains("movementSideways = 0.0F"));
        assertTrue(inputMixin.contains("jumping = false"));
        assertTrue(inputMixin.contains("sneaking = false"));
        assertTrue(mixinConfig.contains("\"client.SpiritualistKeyboardInputMixin\""));
        assertFalse(clientView.contains("ClientPlayNetworking.send"));
        assertFalse(clientView.contains("ServerPlayNetworking"));
        assertFalse(clientView.contains("PayloadTypeRegistry.playS2C"));
        assertFalse(clientView.contains("addEntity"));
        assertFalse(clientView.contains("GameRenderer"));
        assertFalse(client.contains("GameRenderer"));
        assertFalse(mixinConfig.contains("CameraMixin"));
        assertFalse(mixinConfig.contains("GameRendererMixin"));
    }

    @Test
    void spiritualistRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String clientView = Files.readString(CLIENT_VIEW, StandardCharsets.UTF_8);
        String inputMixin = Files.readString(INPUT_MIXIN, StandardCharsets.UTF_8);
        String soundMixin = Files.readString(SOUND_MIXIN, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, payload, client, clientView, inputMixin, soundMixin)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
