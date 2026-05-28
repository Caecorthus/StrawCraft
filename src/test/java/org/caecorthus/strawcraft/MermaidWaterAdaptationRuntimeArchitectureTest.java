package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MermaidWaterAdaptationRuntimeArchitectureTest {
    private static final Path RUNTIME =
            Path.of("src/main/java/org/caecorthus/strawcraft/MermaidWaterAdaptationRuntime.java");
    private static final Path INITIALIZER =
            Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void runtimeRefreshesWaterAdaptationFromServerTickOnly() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get(world)"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(player)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesMermaidWaterAdaptation"));
        assertTrue(runtime.contains("player.isTouchingWater()"));
        assertTrue(runtime.contains("player.isSubmergedInWater()"));
        assertTrue(runtime.contains("StatusEffects.DOLPHINS_GRACE"));
        assertTrue(runtime.contains("StatusEffects.NIGHT_VISION"));
        assertTrue(runtime.contains("WatheAttributes.MAX_SPRINT_TIME"));
    }

    @Test
    void runtimeUsesStableModifierIdAndCleanupPaths() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("MAX_SPRINT_TIME_MODIFIER_ID"));
        assertTrue(runtime.contains("StrawCraft.id(\"mermaid_water_adaptation_max_sprint_time\")"));
        assertTrue(runtime.contains("hasModifier(MAX_SPRINT_TIME_MODIFIER_ID)"));
        assertTrue(runtime.contains("removeModifier(MAX_SPRINT_TIME_MODIFIER_ID)"));
        assertTrue(runtime.contains("clearPlayer(player)"));
        assertTrue(runtime.contains("clearWorld(serverWorld)"));
        assertTrue(runtime.contains("GameEvents.ON_FINISH_INITIALIZE.register"));
        assertTrue(runtime.contains("GameEvents.ON_FINISH_FINALIZE.register"));
    }

    @Test
    void strawCraftRegistersRuntimeAfterNoellesRolesExist() throws IOException {
        String initializer = Files.readString(INITIALIZER, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("MermaidWaterAdaptationRuntime.registerEvents()"));
        assertTrue(initializer.indexOf("NoellesRoleCatalog.registerWithWathe()")
                < initializer.indexOf("MermaidWaterAdaptationRuntime.registerEvents()"));
    }

    @Test
    void runtimeAvoidsSparkNoellesParoxDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertFalse(runtime.contains("org.trainmurdermystery"));
        assertFalse(runtime.contains("org.noellesroles"));
        assertFalse(runtime.contains("dev.doctor4t.noellesroles"));
        assertFalse(runtime.contains("XruiDD"));
        assertFalse(runtime.contains("wathe-Parox"));
    }
}
