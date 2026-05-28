package org.caecorthus.strawcraft.role;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.NoellesRoleCatalog;
import org.caecorthus.strawcraft.StrawCraft;
import org.caecorthus.strawcraft.WatheRoleIds;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleSelectionPolicyTest {
    private static final Identifier CIVILIAN = WatheRoleIds.CIVILIAN;
    private static final Identifier KILLER = WatheRoleIds.KILLER;
    private static final Identifier LOOSE_END = WatheRoleIds.LOOSE_END;
    private static final Identifier WITCH = Identifier.of("strawcraft", "witch_fixture");
    private static final Identifier DISABLED_WITCH = Identifier.of("strawcraft", "disabled_witch_fixture");
    private static final Identifier VIGILANTE = WatheRoleIds.VIGILANTE;

    @Test
    void assignsTargetCountsAndFallsBackToCivilianForRemainingPlayers() {
        List<UUID> players = players(6);
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(players, 1, 1, 1, 1, Set.of(), Map.of());

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions());

        assertEquals(6, plan.assignments().size());
        assertEquals(1, plan.count(StrawFaction.KILLER, definitions()));
        assertEquals(1, plan.count(StrawFaction.NEUTRAL, definitions()));
        assertEquals(1, plan.count(StrawFaction.WITCH, definitions()));
        assertEquals(3, plan.count(StrawFaction.GOOD, definitions()));
        assertTrue(plan.assignments().containsValue(WITCH));
        assertEquals(CIVILIAN, plan.assignments().get(players.get(5)));
    }

    @Test
    void disabledRolesAreSkippedAndUniqueSpecialRolesAreAssignedOnce() {
        List<UUID> players = players(6);
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(players, 1, 0, 2, 0, Set.of(DISABLED_WITCH), Map.of());

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions());

        assertEquals(1, plan.count(StrawFaction.WITCH, definitions()));
        assertTrue(plan.assignments().containsValue(WITCH));
        assertFalse(plan.assignments().containsValue(DISABLED_WITCH));
    }

    @Test
    void existingAssignmentsReduceFactionTargetsAndStayInPlace() {
        List<UUID> players = players(5);
        UUID forcedKiller = players.getFirst();
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(
                players,
                1,
                1,
                0,
                0,
                Set.of(),
                Map.of(forcedKiller, KILLER)
        );

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions());

        assertEquals(KILLER, plan.assignments().get(forcedKiller));
        assertEquals(1, plan.count(StrawFaction.KILLER, definitions()));
        assertEquals(1, plan.count(StrawFaction.NEUTRAL, definitions()));
    }

    @Test
    void existingRepeatableAssignmentsDoNotRemoveDefinitionBeforeQuotaIsMet() {
        List<UUID> players = players(4);
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(
                players,
                2,
                0,
                0,
                0,
                Set.of(),
                Map.of(players.getFirst(), KILLER)
        );

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions());

        assertEquals(KILLER, plan.assignments().get(players.getFirst()));
        assertEquals(KILLER, plan.assignments().get(players.get(1)));
        assertEquals(2, plan.count(StrawFaction.KILLER, definitions()));
    }

    @Test
    void appearanceFalseDefinitionIsNotAssigned() {
        Identifier hiddenNeutral = Identifier.of("strawcraft", "hidden_neutral_fixture");
        List<StrawRoleDefinition> definitions = List.of(
                new StrawRoleDefinition(CIVILIAN, StrawFaction.GOOD, true, false, context -> true),
                new StrawRoleDefinition(hiddenNeutral, StrawFaction.NEUTRAL, false, true, context -> false)
        );
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(players(3), 0, 1, 0, 0, Set.of(), Map.of());

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions);

        assertFalse(plan.assignments().containsValue(hiddenNeutral));
        assertEquals(0, plan.count(StrawFaction.NEUTRAL, definitions));
    }

    @Test
    void negativeTargetsClampToNoSpecialAssignments() {
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(players(3), -1, -1, -1, -1, Set.of(), Map.of());

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions());

        assertEquals(0, plan.count(StrawFaction.KILLER, definitions()));
        assertEquals(0, plan.count(StrawFaction.NEUTRAL, definitions()));
        assertEquals(0, plan.count(StrawFaction.WITCH, definitions()));
        assertEquals(3, plan.count(StrawFaction.GOOD, definitions()));
    }

    @Test
    void playerShortageStopsAssignmentWithoutRepeatingPlayers() {
        List<UUID> players = players(2);
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(players, 3, 3, 3, 3, Set.of(), Map.of());

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions());

        assertEquals(2, plan.assignments().size());
        assertTrue(plan.assignments().keySet().containsAll(players));
    }

    @Test
    void witchFactionIsExpressiveWithoutImportingNoellesRolesConcreteRoles() {
        StrawRoleDefinition witch = new StrawRoleDefinition(
                WITCH,
                StrawFaction.WITCH,
                false,
                true,
                context -> true
        );

        assertEquals(StrawFaction.WITCH, witch.faction());
        assertFalse(witch.id().getNamespace().equals("noellesroles"));
    }

    @Test
    void runtimeNoellesDefinitionsAssignImplementedRolesButSkipDisabledAndDeferredRoles() {
        List<UUID> players = players(19);
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(
                players,
                5,
                0,
                0,
                14,
                NoellesRoleCatalog.runtimeSelectionDisabledIds(),
                Map.of()
        );
        List<StrawRoleDefinition> definitions = new java.util.ArrayList<>(NoellesRoleCatalog.runtimeSelectionDefinitions());
        definitions.add(new StrawRoleDefinition(CIVILIAN, StrawFaction.GOOD, true, false, unused -> true));

        RoleSelectionPolicy.SelectionPlan plan = RoleSelectionPolicy.assign(context, definitions);

        assertTrue(plan.assignments().containsValue(StrawCraft.id("swapper")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("phantom")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("bomber")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("scavenger")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("poisoner")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("timekeeper")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("conductor")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("noisemaker")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("voodoo")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("coroner")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("recaller")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("toxicologist")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("reporter")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("professor")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("attendant")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("bodyguard")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("engineer")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("detective")));
        assertTrue(plan.assignments().containsValue(StrawCraft.id("survival_master")));
        assertFalse(plan.assignments().containsValue(StrawCraft.id("time_keeper")));
        assertFalse(plan.assignments().containsValue(StrawCraft.id("awesome_binglus")));
        assertFalse(plan.assignments().containsValue(StrawCraft.id("undercover")));
        assertFalse(plan.assignments().containsValue(StrawCraft.id("jester")));
        assertFalse(plan.assignments().containsValue(StrawCraft.id("vulture")));
    }

    private static List<StrawRoleDefinition> definitions() {
        return List.of(
                new StrawRoleDefinition(KILLER, StrawFaction.KILLER, true, false, context -> true),
                new StrawRoleDefinition(VIGILANTE, StrawFaction.GOOD, false, true, context -> true),
                new StrawRoleDefinition(CIVILIAN, StrawFaction.GOOD, true, false, context -> true),
                new StrawRoleDefinition(LOOSE_END, StrawFaction.NEUTRAL, false, true, context -> true),
                new StrawRoleDefinition(WITCH, StrawFaction.WITCH, false, true, context -> true),
                new StrawRoleDefinition(DISABLED_WITCH, StrawFaction.WITCH, false, true, context -> true)
        );
    }

    private static List<UUID> players(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> new UUID(0, index + 1L))
                .toList();
    }
}
