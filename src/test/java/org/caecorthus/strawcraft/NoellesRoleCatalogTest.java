package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesRoleCatalogTest {
    private static final Set<String> EXPECTED_RUNTIME_READY_ROLE_PATHS = Set.of(
            "swapper",
            "phantom",
            "bomber",
            "scavenger",
            "assassin",
            "silencer",
            "poisoner",
            "bandit",
            "timekeeper",
            "undercover",
            "conductor",
            "bartender",
            "noisemaker",
            "voodoo",
            "coroner",
            "recaller",
            "toxicologist",
            "reporter",
            "professor",
            "attendant",
            "bodyguard",
            "engineer",
            "detective",
            "survival_master",
            "waiter",
            "mermaid",
            "demon_hunter",
            "vulture",
            "pathogen"
    );
    private static final Set<String> EXPECTED_DESIGN_REQUIRED_ROLE_PATHS = Set.of(
            "morphling",
            "serial_killer",
            "time_keeper",
            "spiritualist",
            "corrupt_cop",
            "taotie"
    );
    private static final Set<String> EXPECTED_UNSUPPORTED_ROLE_PATHS = Set.of();
    private static final Set<String> EXPECTED_DISABLED_ROLE_PATHS = Set.of(
            "the_insane_damned_paranoid_killer",
            "awesome_binglus",
            "jester"
    );
    private static final List<String> EXPECTED_ROLE_PATHS = List.of(
            "swapper",
            "phantom",
            "morphling",
            "the_insane_damned_paranoid_killer",
            "bomber",
            "assassin",
            "scavenger",
            "serial_killer",
            "silencer",
            "poisoner",
            "bandit",
            "timekeeper",
            "time_keeper",
            "undercover",
            "conductor",
            "awesome_binglus",
            "bartender",
            "noisemaker",
            "voodoo",
            "coroner",
            "recaller",
            "toxicologist",
            "reporter",
            "professor",
            "attendant",
            "bodyguard",
            "survival_master",
            "engineer",
            "spiritualist",
            "detective",
            "waiter",
            "mermaid",
            "demon_hunter",
            "jester",
            "vulture",
            "corrupt_cop",
            "pathogen",
            "taotie"
    );

    @Test
    void catalogContainsExpectedStrawCraftRoleIdsExactlyOnce() {
        Set<Identifier> expected = EXPECTED_ROLE_PATHS.stream()
                .map(StrawCraft::id)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        List<Identifier> actual = NoellesRoleCatalog.all().stream()
                .map(NoellesRoleCatalog.Entry::id)
                .toList();

        assertEquals(expected, Set.copyOf(actual));
        assertEquals(actual.size(), Set.copyOf(actual).size());
        assertTrue(actual.stream().allMatch(id -> id.getNamespace().equals(StrawCraft.MOD_ID)));
        assertFalse(actual.stream().anyMatch(id -> id.getNamespace().equals("noellesroles")));
    }

    @Test
    void disabledRolesAreNotEligibleForFirstRoundSelection() {
        Set<Identifier> firstRoundIds = NoellesRoleCatalog.firstRoundDefinitions().stream()
                .map(org.caecorthus.strawcraft.role.StrawRoleDefinition::id)
                .collect(Collectors.toSet());

        assertFalse(firstRoundIds.contains(StrawCraft.id("the_insane_damned_paranoid_killer")));
        assertFalse(firstRoundIds.contains(StrawCraft.id("awesome_binglus")));
        assertFalse(firstRoundIds.contains(StrawCraft.id("jester")));
        assertFalse(firstRoundIds.contains(StrawCraft.id("riot_patrol")));
    }

    @Test
    void runtimeSelectionDefinitionsAreExactlyTheHardenedSafeRoles() {
        Set<Identifier> runtimeIds = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(org.caecorthus.strawcraft.role.StrawRoleDefinition::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                StrawCraft.id("swapper"),
                StrawCraft.id("phantom"),
                StrawCraft.id("bomber"),
                StrawCraft.id("scavenger"),
                StrawCraft.id("assassin"),
                StrawCraft.id("silencer"),
                StrawCraft.id("poisoner"),
                StrawCraft.id("bandit"),
                StrawCraft.id("timekeeper"),
                StrawCraft.id("undercover"),
                StrawCraft.id("conductor"),
                StrawCraft.id("bartender"),
                StrawCraft.id("noisemaker"),
                StrawCraft.id("voodoo"),
                StrawCraft.id("coroner"),
                StrawCraft.id("recaller"),
                StrawCraft.id("toxicologist"),
                StrawCraft.id("reporter"),
                StrawCraft.id("professor"),
                StrawCraft.id("attendant"),
                StrawCraft.id("bodyguard"),
                StrawCraft.id("engineer"),
                StrawCraft.id("detective"),
                StrawCraft.id("survival_master"),
                StrawCraft.id("waiter"),
                StrawCraft.id("mermaid"),
                StrawCraft.id("demon_hunter"),
                StrawCraft.id("vulture"),
                StrawCraft.id("pathogen")
        ), runtimeIds);
    }

    @Test
    void roleReadinessExplicitlySeparatesRuntimeReadyDesignRequiredUnsupportedAndDisabledRoles() {
        assertEquals(
                ids(EXPECTED_RUNTIME_READY_ROLE_PATHS),
                idsWithReadiness(NoellesRoleCatalog.Readiness.RUNTIME_READY)
        );
        assertEquals(
                ids(EXPECTED_DESIGN_REQUIRED_ROLE_PATHS),
                idsWithReadiness(NoellesRoleCatalog.Readiness.DESIGN_REQUIRED)
        );
        assertEquals(
                ids(EXPECTED_UNSUPPORTED_ROLE_PATHS),
                idsWithReadiness(NoellesRoleCatalog.Readiness.UNSUPPORTED)
        );
        assertEquals(
                ids(EXPECTED_DISABLED_ROLE_PATHS),
                idsWithReadiness(NoellesRoleCatalog.Readiness.DISABLED)
        );
    }

    @Test
    void runtimeSelectionCandidatesComeOnlyFromRuntimeReadyReadiness() {
        Set<Identifier> runtimeReadyIds = NoellesRoleCatalog.all().stream()
                .filter(entry -> entry.readiness().isRuntimeReady())
                .map(NoellesRoleCatalog.Entry::id)
                .collect(Collectors.toSet());
        Set<Identifier> runtimeSelectionIds = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(org.caecorthus.strawcraft.role.StrawRoleDefinition::id)
                .collect(Collectors.toSet());

        assertEquals(runtimeReadyIds, runtimeSelectionIds);
        assertTrue(runtimeSelectionIds.contains(StrawCraft.id("assassin")));
        assertFalse(runtimeSelectionIds.contains(StrawCraft.id("morphling")));
        assertFalse(runtimeSelectionIds.contains(StrawCraft.id("spiritualist")));
        assertTrue(runtimeSelectionIds.contains(StrawCraft.id("bandit")));
        assertFalse(runtimeSelectionIds.contains(StrawCraft.id("corrupt_cop")));
        assertTrue(runtimeSelectionIds.contains(StrawCraft.id("pathogen")));
        assertFalse(runtimeSelectionIds.contains(StrawCraft.id("taotie")));
        assertFalse(runtimeSelectionIds.contains(StrawCraft.id("jester")));
        assertTrue(runtimeSelectionIds.contains(StrawCraft.id("undercover")));
        assertTrue(runtimeSelectionIds.contains(StrawCraft.id("demon_hunter")));
    }

    @Test
    void undercoverWatheRoleIsInnocentNoMoodAndNoKillerTools() {
        Role role = NoellesRoleCatalog.find(StrawCraft.id("undercover")).orElseThrow().watheRole();

        assertTrue(role.isInnocent());
        assertFalse(role.canUseKiller());
        assertEquals(Role.MoodType.NONE, role.getMoodType());
        assertEquals(200, role.getMaxSprintTime());
    }

    @Test
    void languageFilesCoverRegisteredAnnouncementRolesAndGoals() throws java.io.IOException {
        String english = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/assets/strawcraft/lang/en_us.json"));
        String chinese = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json"));

        for (String path : EXPECTED_ROLE_PATHS) {
            assertTrue(english.contains("\"announcement.role." + path + "\""));
            assertTrue(english.contains("\"announcement.goal." + path + "\""));
            assertTrue(english.contains("\"announcement.goals." + path + "\""));
            assertTrue(chinese.contains("\"announcement.role." + path + "\""));
            assertTrue(chinese.contains("\"announcement.goal." + path + "\""));
            assertTrue(chinese.contains("\"announcement.goals." + path + "\""));
        }
    }

    private static Set<Identifier> idsWithReadiness(NoellesRoleCatalog.Readiness readiness) {
        return NoellesRoleCatalog.all().stream()
                .filter(entry -> entry.readiness() == readiness)
                .map(NoellesRoleCatalog.Entry::id)
                .collect(Collectors.toSet());
    }

    private static Set<Identifier> ids(Set<String> paths) {
        return paths.stream()
                .map(StrawCraft::id)
                .collect(Collectors.toSet());
    }
}
