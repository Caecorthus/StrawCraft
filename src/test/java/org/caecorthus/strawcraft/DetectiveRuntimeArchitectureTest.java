package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectiveRuntimeArchitectureTest {
    @Test
    void detectiveKillHistoryRuntimeRegistersAndClearsOnWatheRoundBoundaries() throws IOException {
        String strawCraft = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"),
                StandardCharsets.UTF_8
        );
        String officialBridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(strawCraft.contains("DetectiveKillHistoryRuntime.registerEvents()"));
        assertTrue(officialBridge.contains("DetectiveKillHistoryRuntime.resetAll();"));
        assertTrue(officialBridge.indexOf("GameEvents.ON_FINISH_INITIALIZE")
                < officialBridge.indexOf("DetectiveKillHistoryRuntime.resetAll();"));
        assertTrue(officialBridge.indexOf("GameEvents.ON_FINISH_FINALIZE")
                < officialBridge.lastIndexOf("DetectiveKillHistoryRuntime.resetAll();"));
    }

    @Test
    void detectiveInvestigationHasProductionC2SAndClientTrigger() throws IOException {
        String runtime = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/DetectiveKillHistoryRuntime.java"),
                StandardCharsets.UTF_8
        );
        String payload = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/DetectiveInvestigationPayload.java"),
                StandardCharsets.UTF_8
        );
        String client = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(DetectiveInvestigationPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(DetectiveInvestigationPayload.ID"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival"));
        assertTrue(runtime.contains("canSee"));
        assertTrue(runtime.contains("squaredDistanceTo"));
        assertTrue(runtime.contains("tryBeginInvestigationCooldown"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(detective)"));
        assertTrue(runtime.contains("DETECTIVE_INVESTIGATE_COOLDOWN"));
        assertTrue(runtime.contains("Text.translatable"));
        assertTrue(payload.contains("detective_investigate"));
        assertTrue(client.contains("detectiveInvestigateKey"));
        assertTrue(client.contains("ClientPlayNetworking.send(new DetectiveInvestigationPayload"));
    }

    @Test
    void detectiveRuntimeRecordsOfficialCompletedDeathsWithoutSparkRuntimeEvents() throws IOException {
        String runtime = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/DetectiveKillHistoryRuntime.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(runtime.contains("StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.register"));
        assertTrue(runtime.contains("recordOfficialDeath"));
        assertFalse(runtime.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register"));
        assertFalse(runtime.contains("KillPlayer.AFTER.register"));
        assertFalse(runtime.contains("import org.trainmurdermystery"));
        assertFalse(runtime.contains("import org.noellesroles"));
    }
}
