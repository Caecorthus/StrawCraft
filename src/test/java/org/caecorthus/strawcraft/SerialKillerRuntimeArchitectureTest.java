package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialKillerRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/SerialKillerRuntime.java");
    private static final Path POLICY = Path.of("src/main/java/org/caecorthus/strawcraft/SerialKillerTargetPolicy.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void runtimeHooksRoleAssignmentDeathRewardAndPeriodicTargetValidation() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("SerialKillerRuntime.register()"));
        assertTrue(runtime.contains("StrawRoleEvents.ROLE_ASSIGNED.register"));
        assertTrue(runtime.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register"));
        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register(SerialKillerRuntime::tickServer)"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(serialKiller)"));
        assertTrue(runtime.contains("roleState.setSerialKillerCurrentTarget"));
        assertTrue(runtime.contains("roleState.clearSerialKillerCurrentTarget"));
        assertTrue(runtime.contains("SerialKillerTargetPolicy.assignTarget"));
        assertTrue(runtime.contains("SerialKillerTargetPolicy.isTargetValid"));
        assertTrue(runtime.contains("SerialKillerTargetPolicy.bonusGrant"));
        assertTrue(runtime.contains("KillRewardPayout.apply"));
    }

    @Test
    void policyKeepsSourceBackedBonusAndNoClientHighlightClaims() throws IOException {
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);

        assertTrue(policy.contains("BONUS_MONEY = 50"));
        assertTrue(policy.contains("swallowed"));
        assertTrue(policy.contains("undercover"));
        assertTrue(policy.contains("bodyguard"));
        assertTrue(policy.contains("survivalMaster"));
        assertFalse(policy.contains("Client"));
        assertFalse(policy.contains("Render"));
    }

    @Test
    void serialKillerRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, policy)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
