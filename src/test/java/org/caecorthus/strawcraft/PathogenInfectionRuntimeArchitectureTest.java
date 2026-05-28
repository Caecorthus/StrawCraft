package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathogenInfectionRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/PathogenInfectionRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/PathogenInfectionPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");
    private static final Path LOADOUTS = Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java");

    @Test
    void payloadCarriesOnlyEmptyIntentAndNoClientTargetClaims() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record PathogenInfectionPayload()"));
        assertTrue(payload.contains("PacketCodec.unit(new PathogenInfectionPayload())"));
        assertFalse(payload.contains("UUID"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
        assertFalse(payload.contains("Role"));
    }

    @Test
    void runtimeOwnsTargetSearchValidationInfectionCooldownAndNeutralClaim() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(PathogenInfectionPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(PathogenInfectionPayload.ID"));
        assertTrue(runtime.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(pathogen)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesPathogenInfection"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(pathogen)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(PathogenInfectionPolicy.ABILITY_ID"));
        assertTrue(runtime.contains("pathogen.canSee(target)"));
        assertTrue(runtime.contains("targetState.setPathogenInfectedBy(pathogen.getUuid())"));
        assertTrue(runtime.contains("roleState.tryBeginAbilityCooldown"));
        assertTrue(runtime.contains("PathogenWinPolicy.recordNeutralWinIfComplete"));
        assertTrue(runtime.indexOf("PathogenInfectionPolicy.evaluate") < runtime.indexOf("targetState.setPathogenInfectedBy"));
    }

    @Test
    void commonAssignmentAndClientRegistrationUseIntentOnlyKeybind() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String loadouts = Files.readString(LOADOUTS, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("PathogenInfectionRuntime.register()"));
        assertTrue(loadouts.contains("PathogenInfectionPolicy.resetParticipantState"));
        assertTrue(loadouts.contains("PathogenInfectionPolicy.resetPathogenState"));
        assertTrue(client.contains("\"key.strawcraft.pathogen_infect\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickPathogenInfection)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new PathogenInfectionPayload())"));
        assertFalse(client.contains("new PathogenInfectionPayload(target"));
    }

    @Test
    void pathogenRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
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
