package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JesterRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/JesterRuntime.java");

    @Test
    void runtimeUsesOfficialWatheAndStrawCraftEventsOnly() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("ServerTickEvents.END_SERVER_TICK.register"));
        assertTrue(source.contains("StrawKillEvents.BEFORE_KILL.register"));
        assertTrue(source.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register"));
        assertTrue(source.contains("StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register"));
        assertTrue(source.contains("JesterWinPolicy.evaluateKillAttempt"));
        assertTrue(source.contains("JesterWinPolicy.evaluateWinCheck"));
        assertFalse(source.contains("KillPlayer.AFTER"));
        assertFalse(source.contains("CheckWinCondition"));
        assertFalse(source.contains("noellesroles"));
        assertFalse(source.contains("Parox"));
    }

    @Test
    void runtimeOwnsStasisPsychoTargetDeathAndLooseEndWinClaim() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String roundEndMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/GameRoundEndComponentMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("new NoellesRoleState.JesterMomentState"));
        assertTrue(source.contains("targetDeathCompletesWin"));
        assertTrue(source.contains("TARGET_KILLED_TRIGGER"));
        assertTrue(source.contains("replaceDefaultWin(StrawWinEvents.DefaultWin.LOOSE_END)"));
        assertTrue(source.contains("killJesterAfterTimeout"));
        assertTrue(source.indexOf("setArmour(0)") < source.indexOf("GameFunctions.killPlayer(player, true, null, StrawDeathReasons.JESTER_TIMEOUT)"));
        assertTrue(source.indexOf("setPsychoTicks(0)") < source.indexOf("GameFunctions.killPlayer(player, true, null, StrawDeathReasons.JESTER_TIMEOUT)"));
        assertTrue(roundEndMixin.contains("canOverrideLooseEndWinner"));
    }

    @Test
    void strawCraftRegistersJesterRuntimeAfterRoleRewriteBridge() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("JesterRuntime.register()"));
        assertTrue(source.indexOf("WatheOfficialBridge.register()") < source.indexOf("JesterRuntime.register()"));
    }

    @Test
    void jesterIsSelectableAfterRuntimeHooksAreOwned() {
        NoellesRoleCatalog.Entry entry = NoellesRoleCatalog.find(JesterWinPolicy.JESTER_ROLE).orElseThrow();

        assertEquals(NoellesRoleCatalog.Readiness.RUNTIME_READY, entry.readiness());
        assertFalse(NoellesRoleCatalog.runtimeSelectionDisabledIds().contains(entry.id()));
        assertTrue(NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .anyMatch(definition -> definition.id().equals(entry.id())));
    }

    @Test
    void roleAssignmentInitializesJesterMomentState() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("JesterWinPolicy.resetParticipantState"));
        assertTrue(source.indexOf("JesterWinPolicy.resetParticipantState")
                < source.indexOf("NoellesAssignedLoadouts.giveAssignedItems"));
    }
}
