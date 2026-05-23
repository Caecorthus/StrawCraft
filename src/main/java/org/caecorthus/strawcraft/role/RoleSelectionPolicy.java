package org.caecorthus.strawcraft.role;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RoleSelectionPolicy {
    private RoleSelectionPolicy() {
    }

    public static SelectionPlan assign(StrawRoleSelectionContext context, List<StrawRoleDefinition> definitions) {
        Map<Identifier, StrawRoleDefinition> definitionsById = definitions.stream()
                .collect(Collectors.toMap(StrawRoleDefinition::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        LinkedHashMap<UUID, Identifier> assignments = new LinkedHashMap<>();
        for (UUID player : context.players()) {
            Identifier existingRole = context.existingAssignments().get(player);
            if (existingRole != null && !context.disabledRoles().contains(existingRole)) {
                assignments.put(player, existingRole);
            }
        }

        assignFaction(context, definitions, definitionsById, assignments, StrawFaction.KILLER);
        assignFaction(context, definitions, definitionsById, assignments, StrawFaction.NEUTRAL);
        assignFaction(context, definitions, definitionsById, assignments, StrawFaction.WITCH);
        assignFaction(context, definitions, definitionsById, assignments, StrawFaction.GOOD);
        fillFallbackGoodRoles(context, definitions, assignments);

        return new SelectionPlan(Map.copyOf(assignments));
    }

    private static void assignFaction(
            StrawRoleSelectionContext context,
            List<StrawRoleDefinition> definitions,
            Map<Identifier, StrawRoleDefinition> definitionsById,
            LinkedHashMap<UUID, Identifier> assignments,
            StrawFaction faction
    ) {
        int remaining = context.targetCountFor(faction) - count(assignments, definitionsById, faction);
        if (remaining <= 0) {
            return;
        }

        List<StrawRoleDefinition> factionDefinitions = new ArrayList<>(definitions.stream()
                .filter(definition -> definition.faction() == faction)
                .filter(definition -> faction != StrawFaction.GOOD || !definition.fallback())
                .filter(definition -> definition.canAppear(context))
                .filter(definition -> !definition.uniqueSpecial() || !assignments.containsValue(definition.id()))
                .toList());
        for (UUID player : unassignedPlayers(context, assignments)) {
            if (remaining <= 0 || factionDefinitions.isEmpty()) {
                return;
            }
            StrawRoleDefinition definition = factionDefinitions.removeFirst();
            assignments.put(player, definition.id());
            remaining--;
            if (!definition.uniqueSpecial()) {
                factionDefinitions.add(definition);
            }
        }
    }

    private static void fillFallbackGoodRoles(
            StrawRoleSelectionContext context,
            List<StrawRoleDefinition> definitions,
            LinkedHashMap<UUID, Identifier> assignments
    ) {
        Optional<StrawRoleDefinition> fallbackGoodRole = definitions.stream()
                .filter(definition -> definition.faction() == StrawFaction.GOOD)
                .filter(StrawRoleDefinition::fallback)
                .filter(definition -> definition.canAppear(context))
                .findFirst();
        if (fallbackGoodRole.isEmpty()) {
            return;
        }
        Identifier fallbackRole = fallbackGoodRole.get().id();
        for (UUID player : unassignedPlayers(context, assignments)) {
            assignments.put(player, fallbackRole);
        }
    }

    private static List<UUID> unassignedPlayers(StrawRoleSelectionContext context, Map<UUID, Identifier> assignments) {
        return context.players().stream()
                .filter(player -> !assignments.containsKey(player))
                .toList();
    }

    private static int count(
            Map<UUID, Identifier> assignments,
            Map<Identifier, StrawRoleDefinition> definitionsById,
            StrawFaction faction
    ) {
        int total = 0;
        for (Identifier roleId : assignments.values()) {
            StrawRoleDefinition definition = definitionsById.get(roleId);
            if (definition != null && definition.faction() == faction) {
                total++;
            }
        }
        return total;
    }

    public record SelectionPlan(Map<UUID, Identifier> assignments) {
        public SelectionPlan {
            assignments = Map.copyOf(assignments);
        }

        public int count(StrawFaction faction, List<StrawRoleDefinition> definitions) {
            Set<Identifier> factionRoles = definitions.stream()
                    .filter(definition -> definition.faction() == faction)
                    .map(StrawRoleDefinition::id)
                    .collect(Collectors.toSet());
            int total = 0;
            for (Identifier roleId : assignments.values()) {
                if (factionRoles.contains(roleId)) {
                    total++;
                }
            }
            return total;
        }
    }
}
