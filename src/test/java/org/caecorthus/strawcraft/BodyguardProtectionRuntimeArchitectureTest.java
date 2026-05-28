package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyguardProtectionRuntimeArchitectureTest {
    @Test
    void bodyguardProtectionIsRegisteredOnTheOfficialKillPreventionSeam() throws IOException {
        String strawCraft = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"));
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/BodyguardProtectionRuntime.java"));

        assertTrue(strawCraft.contains("BodyguardProtectionRuntime.registerEvents()"));
        assertTrue(runtime.contains("StrawKillEvents.BEFORE_KILL.register"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get(victim.getWorld())"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(bodyguard)"));
        assertTrue(runtime.contains("BodyguardProtectionPolicy.beforeKill"));
    }

    @Test
    void bodyguardProtectionRuntimeDoesNotUseSparkOrNoellesRuntimeHooks() throws IOException {
        String protection = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/BodyguardProtectionPolicy.java"));
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/BodyguardProtectionRuntime.java"));

        assertFalse(protection.contains("XruiDD"));
        assertFalse(protection.contains("NoellesRoles"));
        assertFalse(runtime.contains("XruiDD"));
        assertFalse(runtime.contains("NoellesRoles"));
        assertFalse(runtime.contains("KillPlayer"));
    }
}
