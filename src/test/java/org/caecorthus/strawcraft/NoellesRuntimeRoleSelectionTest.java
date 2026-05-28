package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawRoleDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesRuntimeRoleSelectionTest {
    @Test
    void runtimeSelectionKeepsOfficialSeatsAndAssignsEverySafeImplementedNoellesRole() {
        UUID firstCivilian = new UUID(0, 1);
        UUID firstKiller = new UUID(0, 2);
        UUID vigilante = new UUID(0, 3);
        UUID secondCivilian = new UUID(0, 4);
        UUID thirdCivilian = new UUID(0, 5);
        UUID fourthCivilian = new UUID(0, 6);
        UUID fifthCivilian = new UUID(0, 7);
        UUID sixthCivilian = new UUID(0, 8);
        UUID secondKiller = new UUID(0, 9);
        UUID seventhCivilian = new UUID(0, 10);
        UUID thirdKiller = new UUID(0, 11);
        UUID eighthCivilian = new UUID(0, 12);
        UUID ninthCivilian = new UUID(0, 13);
        UUID tenthCivilian = new UUID(0, 14);
        UUID eleventhCivilian = new UUID(0, 15);
        UUID twelfthCivilian = new UUID(0, 16);
        UUID thirteenthCivilian = new UUID(0, 17);
        UUID fourteenthCivilian = new UUID(0, 18);
        UUID fourthKiller = new UUID(0, 19);
        UUID fifteenthCivilian = new UUID(0, 20);
        UUID sixteenthCivilian = new UUID(0, 21);
        UUID seventeenthCivilian = new UUID(0, 22);
        Map<UUID, Identifier> officialAssignments = linkedAssignments(
                entry(firstCivilian, WatheRoleIds.CIVILIAN),
                entry(firstKiller, WatheRoleIds.KILLER),
                entry(vigilante, WatheRoleIds.VIGILANTE),
                entry(secondCivilian, WatheRoleIds.CIVILIAN),
                entry(thirdCivilian, WatheRoleIds.CIVILIAN),
                entry(fourthCivilian, WatheRoleIds.CIVILIAN),
                entry(fifthCivilian, WatheRoleIds.CIVILIAN),
                entry(sixthCivilian, WatheRoleIds.CIVILIAN),
                entry(secondKiller, WatheRoleIds.KILLER),
                entry(seventhCivilian, WatheRoleIds.CIVILIAN),
                entry(thirdKiller, WatheRoleIds.KILLER),
                entry(eighthCivilian, WatheRoleIds.CIVILIAN),
                entry(ninthCivilian, WatheRoleIds.CIVILIAN),
                entry(tenthCivilian, WatheRoleIds.CIVILIAN),
                entry(eleventhCivilian, WatheRoleIds.CIVILIAN),
                entry(twelfthCivilian, WatheRoleIds.CIVILIAN),
                entry(thirteenthCivilian, WatheRoleIds.CIVILIAN),
                entry(fourteenthCivilian, WatheRoleIds.CIVILIAN),
                entry(fourthKiller, WatheRoleIds.KILLER),
                entry(fifteenthCivilian, WatheRoleIds.CIVILIAN),
                entry(sixteenthCivilian, WatheRoleIds.CIVILIAN),
                entry(seventeenthCivilian, WatheRoleIds.CIVILIAN)
        );

        Map<UUID, Identifier> selected = NoellesRuntimeRoleSelection.planAssignmentIds(officialAssignments).assignments();

        assertEquals(StrawCraft.id("swapper"), selected.get(firstKiller));
        assertEquals(StrawCraft.id("phantom"), selected.get(secondKiller));
        assertEquals(StrawCraft.id("bomber"), selected.get(thirdKiller));
        assertEquals(StrawCraft.id("scavenger"), selected.get(fourthKiller));
        assertEquals(WatheRoleIds.VIGILANTE, selected.get(vigilante));
        assertEquals(StrawCraft.id("timekeeper"), selected.get(firstCivilian));
        assertEquals(StrawCraft.id("conductor"), selected.get(secondCivilian));
        assertEquals(StrawCraft.id("bartender"), selected.get(thirdCivilian));
        assertEquals(StrawCraft.id("noisemaker"), selected.get(fourthCivilian));
        assertEquals(StrawCraft.id("voodoo"), selected.get(fifthCivilian));
        assertEquals(StrawCraft.id("coroner"), selected.get(sixthCivilian));
        assertEquals(StrawCraft.id("recaller"), selected.get(seventhCivilian));
        assertEquals(StrawCraft.id("toxicologist"), selected.get(eighthCivilian));
        assertEquals(StrawCraft.id("reporter"), selected.get(ninthCivilian));
        assertEquals(StrawCraft.id("professor"), selected.get(tenthCivilian));
        assertEquals(StrawCraft.id("attendant"), selected.get(eleventhCivilian));
        assertEquals(StrawCraft.id("bodyguard"), selected.get(twelfthCivilian));
        assertEquals(StrawCraft.id("survival_master"), selected.get(thirteenthCivilian));
        assertEquals(StrawCraft.id("engineer"), selected.get(fourteenthCivilian));
        assertEquals(StrawCraft.id("detective"), selected.get(fifteenthCivilian));
        assertEquals(StrawCraft.id("waiter"), selected.get(sixteenthCivilian));
        assertEquals(StrawCraft.id("mermaid"), selected.get(seventeenthCivilian));
    }

    @Test
    void runtimeSelectionCandidatesAreExactlyThePromotedSafeRoles() {
        Set<Identifier> candidateIds = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(StrawRoleDefinition::id)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(
                StrawCraft.id("swapper"),
                StrawCraft.id("phantom"),
                StrawCraft.id("bomber"),
                StrawCraft.id("scavenger"),
                StrawCraft.id("silencer"),
                StrawCraft.id("poisoner"),
                StrawCraft.id("timekeeper"),
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
                StrawCraft.id("vulture")
        ), candidateIds);
        assertFalse(candidateIds.contains(StrawCraft.id("awesome_binglus")));
        assertFalse(candidateIds.contains(StrawCraft.id("undercover")));
        assertFalse(candidateIds.contains(StrawCraft.id("time_keeper")));
        assertFalse(candidateIds.contains(StrawCraft.id("jester")));
        assertFalse(candidateIds.contains(StrawCraft.id("the_insane_damned_paranoid_killer")));
    }

    @Test
    void runtimeSelectionReplacesOfficialLooseEndSeatWithVulture() {
        UUID civilian = new UUID(0, 1);
        UUID killer = new UUID(0, 2);
        UUID looseEnd = new UUID(0, 3);
        Map<UUID, Identifier> officialAssignments = linkedAssignments(
                entry(civilian, WatheRoleIds.CIVILIAN),
                entry(killer, WatheRoleIds.KILLER),
                entry(looseEnd, WatheRoleIds.LOOSE_END)
        );

        Map<UUID, Identifier> selected = NoellesRuntimeRoleSelection.planAssignmentIds(officialAssignments).assignments();

        assertEquals(StrawCraft.id("swapper"), selected.get(killer));
        assertEquals(StrawCraft.id("vulture"), selected.get(looseEnd));
        assertEquals(StrawCraft.id("timekeeper"), selected.get(civilian));
    }

    @Test
    void runtimeSelectionDoesNotAssignVultureWithoutOfficialNeutralSeat() {
        UUID civilian = new UUID(0, 1);
        UUID killer = new UUID(0, 2);
        Map<UUID, Identifier> officialAssignments = linkedAssignments(
                entry(civilian, WatheRoleIds.CIVILIAN),
                entry(killer, WatheRoleIds.KILLER)
        );

        Map<UUID, Identifier> selected = NoellesRuntimeRoleSelection.planAssignmentIds(officialAssignments).assignments();

        assertFalse(selected.containsValue(StrawCraft.id("vulture")));
    }

    @Test
    void runtimeSelectionPreservesExistingNonVanillaAssignmentsWhileReplacingLooseEnd() {
        UUID existingNeutral = new UUID(0, 1);
        UUID looseEnd = new UUID(0, 2);
        UUID killer = new UUID(0, 3);
        Map<UUID, Identifier> officialAssignments = linkedAssignments(
                entry(existingNeutral, StrawCraft.id("corrupt_cop")),
                entry(looseEnd, WatheRoleIds.LOOSE_END),
                entry(killer, WatheRoleIds.KILLER)
        );

        Map<UUID, Identifier> selected = NoellesRuntimeRoleSelection.planAssignmentIds(officialAssignments).assignments();

        assertEquals(StrawCraft.id("corrupt_cop"), selected.get(existingNeutral));
        assertEquals(StrawCraft.id("vulture"), selected.get(looseEnd));
        assertEquals(StrawCraft.id("swapper"), selected.get(killer));
    }

    @Test
    void vanillaRolesRemainValidFallbacksWhenNoNoellesRoleIsEligible() {
        UUID civilian = new UUID(0, 1);
        UUID killer = new UUID(0, 2);
        UUID vigilante = new UUID(0, 3);
        UUID looseEnd = new UUID(0, 4);
        Map<UUID, Identifier> officialAssignments = linkedAssignments(
                entry(civilian, WatheRoleIds.CIVILIAN),
                entry(killer, WatheRoleIds.KILLER),
                entry(vigilante, WatheRoleIds.VIGILANTE),
                entry(looseEnd, WatheRoleIds.LOOSE_END)
        );

        Map<UUID, Identifier> selected = NoellesRuntimeRoleSelection.planAssignmentIds(
                officialAssignments,
                List.of(),
                Set.of()
        ).assignments();

        assertEquals(WatheRoleIds.CIVILIAN, selected.get(civilian));
        assertEquals(WatheRoleIds.KILLER, selected.get(killer));
        assertEquals(WatheRoleIds.VIGILANTE, selected.get(vigilante));
        assertEquals(WatheRoleIds.LOOSE_END, selected.get(looseEnd));
    }

    @Test
    void runtimeRoleObjectsResolveToRegisteredWatheRolesForLoadoutEvents() {
        UUID killer = new UUID(0, 1);
        Map<UUID, Role> selected = NoellesRuntimeRoleSelection.planAssignments(linkedRoleAssignments(
                roleEntry(killer, role(WatheRoleIds.KILLER, false, true))
        ));

        Role assignedRole = selected.get(killer);
        assertEquals(StrawCraft.id("swapper"), assignedRole.identifier());
        assertTrue(StrawRoleMeaning.canUseKillerShop(assignedRole));
    }

    @Test
    void survivalMasterCountdownCountsRewrittenKillerRolesAfterRuntimeSelection() {
        UUID firstCivilian = new UUID(0, 1);
        UUID firstKiller = new UUID(0, 2);
        UUID secondCivilian = new UUID(0, 3);
        UUID secondKiller = new UUID(0, 4);
        Map<UUID, Role> selected = NoellesRuntimeRoleSelection.planAssignments(linkedRoleAssignments(
                roleEntry(firstCivilian, role(WatheRoleIds.CIVILIAN, true, false)),
                roleEntry(firstKiller, role(WatheRoleIds.KILLER, false, true)),
                roleEntry(secondCivilian, role(WatheRoleIds.CIVILIAN, true, false)),
                roleEntry(secondKiller, role(WatheRoleIds.KILLER, false, true))
        ));

        assertEquals(StrawCraft.id("swapper"), selected.get(firstKiller).identifier());
        assertEquals(StrawCraft.id("phantom"), selected.get(secondKiller).identifier());
        assertEquals(2, SurvivalMasterCountdownRuntime.countStartingKillers(selected.values()));
    }

    @Test
    void postAssignmentForcedVanillaKillerOrCivilianCannotBeDistinguishedButKeepsSeatSemantics() {
        UUID maybeForcedKiller = new UUID(0, 1);
        UUID maybeForcedCivilian = new UUID(0, 2);
        Map<UUID, Identifier> officialAssignments = linkedAssignments(
                entry(maybeForcedKiller, WatheRoleIds.KILLER),
                entry(maybeForcedCivilian, WatheRoleIds.CIVILIAN)
        );

        Map<UUID, Identifier> selected = NoellesRuntimeRoleSelection.planAssignmentIds(officialAssignments).assignments();

        assertEquals(StrawCraft.id("swapper"), selected.get(maybeForcedKiller));
        assertEquals(StrawCraft.id("timekeeper"), selected.get(maybeForcedCivilian));
        assertEquals(1, selected.values().stream()
                .filter(roleId -> StrawCraft.id("swapper").equals(roleId) || WatheRoleIds.KILLER.equals(roleId))
                .count());
    }

    @SafeVarargs
    private static Map<UUID, Identifier> linkedAssignments(Map.Entry<UUID, Identifier>... entries) {
        LinkedHashMap<UUID, Identifier> assignments = new LinkedHashMap<>();
        for (Map.Entry<UUID, Identifier> entry : entries) {
            assignments.put(entry.getKey(), entry.getValue());
        }
        return assignments;
    }

    @SafeVarargs
    private static Map<UUID, Role> linkedRoleAssignments(Map.Entry<UUID, Role>... entries) {
        LinkedHashMap<UUID, Role> assignments = new LinkedHashMap<>();
        for (Map.Entry<UUID, Role> entry : entries) {
            assignments.put(entry.getKey(), entry.getValue());
        }
        return assignments;
    }

    private static Map.Entry<UUID, Identifier> entry(UUID player, Identifier role) {
        return Map.entry(player, role);
    }

    private static Map.Entry<UUID, Role> roleEntry(UUID player, Role role) {
        return Map.entry(player, role);
    }

    private static Role role(Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
