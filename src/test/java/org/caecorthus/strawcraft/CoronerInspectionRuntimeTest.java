package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoronerInspectionRuntimeTest {
    @Test
    void coronerInspectionHasProductionPayloadServerBodySearchAndClientIntentKeybind() throws IOException {
        String strawCraft = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"), StandardCharsets.UTF_8);
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/CoronerInspectionRuntime.java"), StandardCharsets.UTF_8);
        String payload = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/CoronerInspectPayload.java"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java"), StandardCharsets.UTF_8);

        assertTrue(strawCraft.contains("CoronerInspectionRuntime.register()"));
        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(CoronerInspectPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(CoronerInspectPayload.ID"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival"));
        assertTrue(runtime.contains("PlayerBodyEntity"));
        assertTrue(runtime.contains("TypeFilter.equals(PlayerBodyEntity.class)"));
        assertTrue(runtime.contains("PlayerBodyEntity::getPlayerUuid"));
        assertTrue(runtime.contains("StrawCorpseMetadata::byDeadPlayer"));
        assertTrue(runtime.contains("ScavengerHiddenBodies.isHiddenBody(world, body)"));
        assertTrue(runtime.contains("ScavengerHiddenBodyVisibility.canSeeBody"));
        assertTrue(runtime.contains("message.strawcraft.coroner.inspect"));
        assertTrue(payload.contains("coroner_inspect"));
        assertTrue(client.contains("coronerInspectKey"));
        assertTrue(client.contains("ClientPlayNetworking.send(new CoronerInspectPayload())"));
    }

    @Test
    void coronerInspectionDoesNotChangeDeathPipelineOrImportSparkNoellesRuntime() throws IOException {
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/CoronerInspectionRuntime.java"), StandardCharsets.UTF_8);
        String payload = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/CoronerInspectPayload.java"), StandardCharsets.UTF_8);

        assertFalse(runtime.contains("body.discard()"));
        assertFalse(runtime.contains("spawnBody"));
        assertFalse(runtime.contains("KillPlayer"));
        assertFalse(runtime.contains("ROLE_DEATH_COMPLETED.register"));
        assertFalse(runtime.contains("NoellesNeutralWin"));
        assertFalse(runtime.contains("killerUuid"));
        assertFalse(runtime.contains("import org.trainmurdermystery"));
        assertFalse(runtime.contains("import org.noellesroles"));
        assertFalse(payload.contains("UUID"));
    }
}
