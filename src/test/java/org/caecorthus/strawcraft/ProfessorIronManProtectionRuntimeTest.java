package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessorIronManProtectionRuntimeTest {
    @Test
    void professorProtectionIsRegisteredOnTheOfficialKillPreventionSeam() throws IOException {
        String strawCraft = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"));
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/ProfessorIronManProtectionRuntime.java"));

        assertTrue(strawCraft.contains("ProfessorIronManProtectionRuntime.registerEvents()"));
        assertTrue(runtime.contains("StrawKillEvents.BEFORE_KILL.register"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(victim)"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get(victim.getWorld()).getRoles().get(victim.getUuid())"));
        assertTrue(runtime.contains("ProfessorIronManProtection.beforeKill"));
    }

    @Test
    void professorProtectionRunsThroughWatheDeathAllowanceBeforeDeathCompletionBookkeeping() throws IOException {
        String bridge = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"));
        int allowanceHook = bridge.indexOf("AllowPlayerDeath.EVENT.register");
        int beforeKillInvoker = bridge.indexOf("StrawKillEvents.BEFORE_KILL.invoker().beforeKill");
        int officialDeathCompleted = bridge.indexOf("OFFICIAL_DEATH_COMPLETED");

        assertTrue(allowanceHook >= 0);
        assertTrue(beforeKillInvoker > allowanceHook);
        assertTrue(bridge.contains("!StrawKillEvents.BEFORE_KILL.invoker().beforeKill"));
        assertFalse(officialDeathCompleted >= 0 && officialDeathCompleted < beforeKillInvoker);
    }

    @Test
    void professorProtectionRuntimeDoesNotUseFakeVialsOrSparkRuntimeHooks() throws IOException {
        String protection = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/ProfessorIronManProtection.java"));
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/ProfessorIronManProtectionRuntime.java"));

        assertFalse(protection.contains("ItemStack"));
        assertFalse(protection.contains("vial"));
        assertFalse(runtime.contains("XruiDD"));
        assertFalse(runtime.contains("NoellesRoles"));
        assertFalse(runtime.contains("KillPlayer"));
    }
}
