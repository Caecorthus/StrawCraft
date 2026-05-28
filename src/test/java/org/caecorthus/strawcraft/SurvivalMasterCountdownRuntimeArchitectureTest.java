package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalMasterCountdownRuntimeArchitectureTest {
    private static final Path RUNTIME =
            Path.of("src/main/java/org/caecorthus/strawcraft/SurvivalMasterCountdownRuntime.java");

    @Test
    void runtimeCompletesThroughOfficialPassengerRoundEndPath() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("GameRoundEndComponent.KEY.get(world)"));
        assertTrue(source.contains("setRoundEndData(world.getPlayers(), GameFunctions.WinStatus.PASSENGERS)"));
        assertTrue(source.contains("GameFunctions.stopGame(world)"));
        assertTrue(source.contains("ServerTickEvents.END_SERVER_TICK.register"));
    }

    @Test
    void runtimeResetsOnOfficialWatheRoundBoundaries() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("GameEvents.ON_FINISH_INITIALIZE.register"));
        assertTrue(source.contains("GameEvents.ON_FINISH_FINALIZE.register"));
        assertTrue(source.contains("resetWorld(serverWorld)"));
    }

    @Test
    void runtimeDoesNotUseSparkWinConditionOrNoellesRuntimeImports() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertFalse(source.contains("CheckWinCondition"));
        assertFalse(source.contains("XruiDD"));
        assertFalse(source.contains("dev.doctor4t.noellesroles"));
        assertFalse(source.contains("org.noellesroles"));
    }

    @Test
    void strawCraftRegistersSurvivalMasterRuntime() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("SurvivalMasterCountdownRuntime.registerEvents()"));
    }
}
