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

class CorruptCopMomentRuntimeArchitectureTest {
    private static final Path RUNTIME =
            Path.of("src/main/java/org/caecorthus/strawcraft/CorruptCopMomentRuntime.java");
    private static final Path OFFICIAL_WATHE_JAR = Path.of("libs/wathe-1.3.2-1.21.1.jar");
    private static final Path PAROX_WATHE_JAR = Path.of("libs/wathe-Parox-1.0.1.jar");

    @Test
    void runtimeUsesOfficialWatheAndStrawCraftEventsOnly() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("ServerTickEvents.END_SERVER_TICK.register"));
        assertTrue(source.contains("StrawKillEvents.BEFORE_KILL.register"));
        assertTrue(source.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register"));
        assertTrue(source.contains("GameFunctions.isPlayerAliveAndSurvival"));
        assertFalse(source.contains("CheckWinCondition"));
        assertFalse(source.contains("KillPlayer.AFTER"));
        assertFalse(source.contains("noellesroles"));
        assertFalse(source.contains("Parox"));
    }

    @Test
    void runtimeAvoidsClientMusicHudAndSparkGunPayloadInFirstSlice() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertFalse(source.contains("WorldMusic"));
        assertFalse(source.contains("inGameHud"));
        assertFalse(source.contains("PlaySoundS2CPacket"));
        assertFalse(source.contains("GunShootPayload"));
    }

    @Test
    void strawCraftRegistersCorruptCopRuntimeAfterRoleRewriteBridge() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("CorruptCopMomentRuntime.register()"));
        assertTrue(source.indexOf("WatheOfficialBridge.register()") < source.indexOf("CorruptCopMomentRuntime.register()"));
    }

    @Test
    void roleAssignmentInitializesCorruptCopMomentState() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("StrawRoleMeaning.receivesCorruptCopMoment"));
        assertTrue(source.contains("CorruptCopMomentPolicy.resetParticipantState"));
        assertTrue(source.contains("CorruptCopMomentPolicy.resetRoundState"));
    }

    @Test
    void officialWatheJarDoesNotProvideSparkWinHookRequiredBySourceCorruptCop() throws IOException {
        String checkWinCondition = "dev/doctor4t/wathe/api/event/CheckWinCondition.class";

        assertFalse(jarContains(OFFICIAL_WATHE_JAR, checkWinCondition));
        assertTrue(jarContains(PAROX_WATHE_JAR, checkWinCondition));
    }

    @Test
    void corruptCopRemainsUnselectableUntilDefaultWinHookIsRuntimeOwned() throws IOException {
        NoellesRoleCatalog.Entry entry = NoellesRoleCatalog.find(CorruptCopMomentPolicy.CORRUPT_COP_ROLE).orElseThrow();
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertEquals(NoellesRoleCatalog.Readiness.DESIGN_REQUIRED, entry.readiness());
        assertTrue(NoellesRoleCatalog.runtimeSelectionDisabledIds().contains(entry.id()));
        assertFalse(runtime.contains("StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register"));
        assertFalse(runtime.contains("CheckWinCondition.EVENT.register"));
    }

    private static boolean jarContains(Path jarPath, String entryName) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            return jar.getEntry(entryName) != null;
        }
    }
}
