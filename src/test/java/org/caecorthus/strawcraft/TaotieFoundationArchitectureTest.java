package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaotieFoundationArchitectureTest {
    private static final Path POLICY = Path.of("src/main/java/org/caecorthus/strawcraft/TaotieSwallowPolicy.java");
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/TaotieSwallowRuntime.java");
    private static final Path CATALOG = Path.of("src/main/java/org/caecorthus/strawcraft/NoellesRoleCatalog.java");
    private static final Path STRAW_CRAFT = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");
    private static final Path OFFICIAL_WATHE_JAR = Path.of("libs/wathe-1.3.2-1.21.1.jar");
    private static final Path PAROX_WATHE_JAR = Path.of("libs/wathe-Parox-1.0.1.jar");

    @Test
    void taotieFoundationNowHasRuntimePacketSpectatorCameraButNoVoiceChatDependency() throws IOException {
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);
        String initializer = Files.readString(STRAW_CRAFT, StandardCharsets.UTF_8);
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(policy.contains("TAOTIE_ROLE"));
        assertTrue(policy.contains("SWALLOW_DISTANCE_SQUARED"));
        assertTrue(policy.contains("TAOTIE_MOMENT_DURATION_TICKS"));
        assertTrue(initializer.contains("TaotieSwallowRuntime.register()"));
        assertTrue(runtime.contains("TaotieSwallowPayload"));
        assertTrue(runtime.contains("GameMode.SPECTATOR"));
        assertTrue(runtime.contains("setCameraEntity(taotie)"));
        assertFalse(policy.contains("ServerPlayNetworking"));
        assertFalse(policy.contains("VoiceChat"));
        assertFalse(runtime.contains("VoiceChat"));
    }

    @Test
    void taotieIsRuntimeReadyAfterGameplayWinAndCleanupHooksAreOwned() throws IOException {
        NoellesRoleCatalog.Entry entry = NoellesRoleCatalog.find(TaotieSwallowPolicy.TAOTIE_ROLE).orElseThrow();
        String catalog = Files.readString(CATALOG, StandardCharsets.UTF_8);

        assertEquals(NoellesRoleCatalog.Readiness.RUNTIME_READY, entry.readiness());
        assertFalse(NoellesRoleCatalog.runtimeSelectionDisabledIds().contains(entry.id()));
        assertTrue(NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .anyMatch(definition -> definition.id().equals(entry.id())));
        assertTrue(catalog.contains("selectableNeutral(\"taotie\")"));
    }

    @Test
    void officialWatheStillLacksSparkCheckWinConditionHookRequiredForTaotieWins() throws IOException {
        String checkWinCondition = "dev/doctor4t/wathe/api/event/CheckWinCondition.class";

        assertFalse(jarContains(OFFICIAL_WATHE_JAR, checkWinCondition));
        assertTrue(jarContains(PAROX_WATHE_JAR, checkWinCondition));
    }

    @Test
    void taotieFoundationAvoidsSparkNoellesAndParoxRuntimeDependencies() throws IOException {
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";

        for (String source : java.util.List.of(policy, runtime)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
            assertFalse(source.contains("CheckWinCondition"));
        }
    }

    private static boolean jarContains(Path jarPath, String entryName) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            return jar.getEntry(entryName) != null;
        }
    }
}
