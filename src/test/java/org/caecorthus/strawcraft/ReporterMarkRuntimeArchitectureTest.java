package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporterMarkRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/ReporterMarkRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/ReporterMarkPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void payloadCarriesExactlyOneTargetUuidAndNoClientCoordinatesOrRoleClaims() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record ReporterMarkPayload(UUID target)"));
        assertTrue(payload.contains("Uuids.PACKET_CODEC"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
        assertFalse(payload.contains("Role"));
    }

    @Test
    void runtimeOwnsRoundRoleAliveTargetCooldownDimensionRangeAndVisibilityChecks() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(ReporterMarkPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(ReporterMarkPayload.ID"));
        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register(ReporterMarkRuntime::tickServer)"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(reporter)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesReporterMark"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(reporter)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(ReporterMarkPolicy.ABILITY_ID"));
        assertTrue(runtime.contains("reporter.getServer().getPlayerManager().getPlayer(payload.target())"));
        assertTrue(runtime.contains("target.getServerWorld() == world"));
        assertTrue(runtime.contains("reporter.squaredDistanceTo(target)"));
        assertTrue(runtime.contains("reporter.canSee(target)"));
        assertTrue(runtime.contains("roleState.setReporterMarkedTarget(target.getUuid())"));
        assertTrue(runtime.contains("roleState.tryBeginAbilityCooldown"));
        assertTrue(runtime.contains("roleState.reporterMarkedTarget()"));
        assertTrue(runtime.contains("message.strawcraft.reporter.tracking"));
        assertTrue(runtime.contains("roleState.clearReporterMarkedTarget()"));
        assertTrue(runtime.indexOf("ReporterMarkPolicy.validate") < runtime.indexOf("roleState.setReporterMarkedTarget"));
    }

    @Test
    void commonAndClientRegistrationUseAimedPlayerUuidSelection() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("ReporterMarkRuntime.register()"));
        assertTrue(client.contains("\"key.strawcraft.reporter_mark\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickReporterMark)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new ReporterMarkPayload(target.getUuid()))"));
    }

    @Test
    void reporterMarkRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
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
