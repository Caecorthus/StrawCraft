package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssassinGuessRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/AssassinGuessRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/AssassinGuessPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path SCREEN = Path.of("src/main/java/org/caecorthus/strawcraft/client/AssassinGuessScreen.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");
    private static final Path LOADOUTS = Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java");

    @Test
    void payloadCarriesOnlyTargetUuidAndGuessedRoleId() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record AssassinGuessPayload(UUID targetUuid, Identifier guessedRoleId)"));
        assertTrue(payload.contains("Uuids.PACKET_CODEC"));
        assertTrue(payload.contains("Identifier.PACKET_CODEC"));
        assertFalse(payload.contains("targetRole"));
        assertFalse(payload.contains("boolean"));
        assertFalse(payload.contains("GameWorldComponent"));
    }

    @Test
    void runtimeRegistersPayloadAndOwnsValidationResolutionAndStateConsumption() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(AssassinGuessPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(AssassinGuessPayload.ID"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(assassin)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesAssassinGuess"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(assassin)"));
        assertTrue(runtime.contains("AssassinGuessPolicy.evaluate"));
        assertTrue(runtime.contains("GameFunctions.killPlayer(target, true, assassin, StrawDeathReasons.ASSASSINATED)"));
        assertTrue(runtime.contains("GameFunctions.killPlayer(assassin, true, null, StrawDeathReasons.ASSASSIN_MISFIRE)"));
        assertTrue(runtime.contains("AssassinGuessPolicy.useGuess(roleState, currentGameTime)"));
        assertTrue(runtime.indexOf("AssassinGuessPolicy.evaluate") < runtime.indexOf("GameFunctions.killPlayer"));
        assertTrue(runtime.indexOf("AssassinGuessPolicy.useGuess") < runtime.indexOf("GameFunctions.killPlayer"));
    }

    @Test
    void initializerAssignmentAndClientUseAssassinRuntimeAndPicker() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String loadouts = Files.readString(LOADOUTS, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String screen = Files.readString(SCREEN, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("AssassinGuessRuntime.register()"));
        assertTrue(loadouts.contains("AssassinGuessPolicy.resetRoundState"));
        assertTrue(client.contains("\"key.strawcraft.assassin_guess\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickAssassinGuess)"));
        assertTrue(client.contains("client.setScreen(new AssassinGuessScreen())"));
        assertTrue(screen.contains("ClientPlayNetworking.send(new AssassinGuessPayload(selectedTarget, roleId))"));
        assertTrue(screen.contains("AssassinGuessPolicy.isGuessableRole(role, true)"));
    }

    @Test
    void assassinRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String screen = Files.readString(SCREEN, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, payload, client, screen)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
