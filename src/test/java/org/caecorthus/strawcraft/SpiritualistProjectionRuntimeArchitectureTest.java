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
    private static final Path ROLE_CATALOG = Path.of("src/main/java/org/caecorthus/strawcraft/NoellesRoleCatalog.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");
    private static final Path LIVING_MIXIN = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/LivingEntityMixin.java");
    private static final Path SOUND_MIXIN = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/ServerCommonNetworkHandlerMixin.java");
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
        assertTrue(client.contains("ClientPlayNetworking.send(new SpiritualistProjectionPayload())"));
        assertFalse(client.contains("new SpiritualistProjectionPayload(target"));
    }

    @Test
    void spiritualistStaysDesignRequiredUntilPlayableClientProjectionExists() throws IOException {
        String catalog = Files.readString(ROLE_CATALOG, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String mixinConfig = Files.readString(MIXIN_CONFIG, StandardCharsets.UTF_8);

        // Runtime state alone is not a playable projection; promotion needs a real camera/freecam/view slice.
        // 仅有运行时状态还不是可玩的投射；提升就绪前需要真实的相机、自由视角或视图切片。
        assertTrue(catalog.contains("good(\"spiritualist\")"));
        assertFalse(catalog.contains("selectableGood(\"spiritualist\")"));
        assertFalse(client.contains("setCameraEntity"));
        assertFalse(client.contains("GameRenderer"));
        assertFalse(client.contains("Camera"));
        assertFalse(client.contains("freecam"));
        assertFalse(client.contains("Freecam"));
        assertFalse(mixinConfig.contains("CameraMixin"));
        assertFalse(mixinConfig.contains("GameRendererMixin"));
    }

    @Test
    void spiritualistRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String soundMixin = Files.readString(SOUND_MIXIN, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, payload, client, soundMixin)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
