package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhantomInvisibilityRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/PhantomInvisibilityRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/PhantomInvisibilityPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void payloadCarriesOnlyEmptyIntentAndNoClientGameplayClaims() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record PhantomInvisibilityPayload()"));
        assertTrue(payload.contains("PacketCodec.unit(new PhantomInvisibilityPayload())"));
        assertFalse(payload.contains("UUID"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
        assertFalse(payload.contains("Role"));
    }

    @Test
    void runtimeOwnsValidationEffectCooldownAndFeedback() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(PhantomInvisibilityPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(PhantomInvisibilityPayload.ID"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(phantom)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesPhantomInvisibility"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(phantom)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(PhantomInvisibilityPolicy.ABILITY_ID"));
        assertTrue(runtime.contains("StatusEffects.INVISIBILITY"));
        assertTrue(runtime.contains("PhantomInvisibilityPolicy.INVISIBILITY_DURATION_TICKS"));
        assertTrue(runtime.contains("roleState.tryBeginAbilityCooldown"));
        assertTrue(runtime.contains("PhantomInvisibilityPolicy.COOLDOWN_TICKS"));
        assertTrue(runtime.contains("\"message.strawcraft.phantom.invisible\""));
        assertTrue(runtime.contains("\"message.strawcraft.phantom.cooldown\""));
        assertTrue(runtime.indexOf("PhantomInvisibilityPolicy.validate") < runtime.indexOf("phantom.addStatusEffect"));
        assertTrue(runtime.indexOf("phantom.addStatusEffect") < runtime.indexOf("roleState.tryBeginAbilityCooldown"));
    }

    @Test
    void commonAndClientRegistrationUseIntentOnlyKeybind() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("PhantomInvisibilityRuntime.register()"));
        assertTrue(client.contains("\"key.strawcraft.phantom_invisibility\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickPhantomInvisibility)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new PhantomInvisibilityPayload())"));
        assertFalse(client.contains("new PhantomInvisibilityPayload(target"));
    }

    @Test
    void phantomRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, payload, client)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
