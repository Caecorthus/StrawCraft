package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorruptCopMomentRuntimeArchitectureTest {
    private static final Path RUNTIME =
            Path.of("src/main/java/org/caecorthus/strawcraft/CorruptCopMomentRuntime.java");

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
}
