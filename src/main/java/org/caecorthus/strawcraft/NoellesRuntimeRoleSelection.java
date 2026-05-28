package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.RoleSelectionPolicy;
import org.caecorthus.strawcraft.role.StrawFaction;
import org.caecorthus.strawcraft.role.StrawRoleDefinition;
import org.caecorthus.strawcraft.role.StrawRoleSelectionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NoellesRuntimeRoleSelection {
    private NoellesRuntimeRoleSelection() {
    }

    public static boolean replaceEligibleOfficialAssignments(GameWorldComponent gameComponent) {
        Map<UUID, Role> currentRoles = gameComponent.getRoles();
        Map<UUID, Role> plannedRoles = planAssignments(currentRoles);
        if (currentRoles.equals(plannedRoles)) {
            return false;
        }

        // Official Wathe syncs before ON_FINISH_INITIALIZE, so this post-assignment rewrite must sync again.
        // 官方 Wathe 会在 ON_FINISH_INITIALIZE 前同步；这里改完职业后需要再同步一次。
        currentRoles.putAll(plannedRoles);
        gameComponent.sync();
        return true;
    }

    static Map<UUID, Role> planAssignments(Map<UUID, Role> officialAssignments) {
        LinkedHashMap<UUID, Identifier> officialAssignmentIds = new LinkedHashMap<>();
        LinkedHashMap<Identifier, Role> rolesById = new LinkedHashMap<>();
        for (Map.Entry<UUID, Role> entry : officialAssignments.entrySet()) {
            Role role = entry.getValue();
            if (role == null) {
                continue;
            }
            Identifier roleId = role.identifier();
            officialAssignmentIds.put(entry.getKey(), roleId);
            rolesById.putIfAbsent(roleId, role);
        }

        RoleSelectionPolicy.SelectionPlan selectedIds = planAssignmentIds(officialAssignmentIds);
        LinkedHashMap<UUID, Role> selectedRoles = new LinkedHashMap<>(officialAssignments);
        for (Map.Entry<UUID, Identifier> entry : selectedIds.assignments().entrySet()) {
            selectedRoles.put(entry.getKey(), roleFor(entry.getValue(), rolesById));
        }
        return selectedRoles;
    }

    static RoleSelectionPolicy.SelectionPlan planAssignmentIds(Map<UUID, Identifier> officialAssignments) {
        return planAssignmentIds(
                officialAssignments,
                NoellesRoleCatalog.runtimeSelectionDefinitions(),
                NoellesRoleCatalog.runtimeSelectionDisabledIds()
        );
    }

    static RoleSelectionPolicy.SelectionPlan planAssignmentIds(
            Map<UUID, Identifier> officialAssignments,
            List<StrawRoleDefinition> runtimeDefinitions,
            Set<Identifier> disabledRoles
    ) {
        List<UUID> players = orderedPlayers(officialAssignments);
        Map<UUID, Identifier> preservedAssignments = preservedAssignments(officialAssignments);
        List<StrawRoleDefinition> definitions = selectionDefinitions(runtimeDefinitions);
        StrawRoleSelectionContext context = new StrawRoleSelectionContext(
                players,
                countFactionSeats(officialAssignments, StrawFaction.KILLER),
                0,
                0,
                countGoodSeats(officialAssignments),
                disabledRoles,
                preservedAssignments
        );
        return RoleSelectionPolicy.assign(context, definitions);
    }

    private static List<StrawRoleDefinition> selectionDefinitions(List<StrawRoleDefinition> runtimeDefinitions) {
        List<StrawRoleDefinition> definitions = new ArrayList<>(runtimeDefinitions);
        // Vanilla roles stay as fallbacks; vigilante is counted if Wathe already assigned it, but not newly replaced.
        // 原版职业保留作兜底；警探只统计官方已分配的席位，不在这里被替换生成。
        definitions.add(new StrawRoleDefinition(WatheRoleIds.KILLER, StrawFaction.KILLER, false, false, context -> true));
        definitions.add(new StrawRoleDefinition(WatheRoleIds.VIGILANTE, StrawFaction.GOOD, false, true, context -> false));
        definitions.add(new StrawRoleDefinition(WatheRoleIds.CIVILIAN, StrawFaction.GOOD, true, false, context -> true));
        return definitions;
    }

    private static Map<UUID, Identifier> preservedAssignments(Map<UUID, Identifier> officialAssignments) {
        LinkedHashMap<UUID, Identifier> preserved = new LinkedHashMap<>();
        for (Map.Entry<UUID, Identifier> entry : officialAssignments.entrySet()) {
            Identifier roleId = entry.getValue();
            if (shouldPreserveOfficialAssignment(roleId)) {
                preserved.put(entry.getKey(), roleId);
            }
        }
        return preserved;
    }

    private static boolean shouldPreserveOfficialAssignment(Identifier roleId) {
        // At this post-assignment hook the official force lists have already been consumed and cleared.
        // 在这个分配后的钩子里，官方强制职业列表已经被消耗并清空。
        // The remaining role map preserves seats, but cannot distinguish forced vanilla killer/civilian.
        // 剩下的职业表只能保留席位语义，无法区分强制指定的原版杀手/平民。
        if (WatheRoleIds.VIGILANTE.equals(roleId)) {
            return true;
        }
        return !WatheRoleIds.KILLER.equals(roleId) && !WatheRoleIds.CIVILIAN.equals(roleId);
    }

    private static List<UUID> orderedPlayers(Map<UUID, Identifier> officialAssignments) {
        return officialAssignments.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<UUID, Identifier> entry) -> seatOrder(entry.getValue()))
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static int seatOrder(Identifier roleId) {
        if (WatheRoleIds.KILLER.equals(roleId)) {
            return 0;
        }
        if (WatheRoleIds.VIGILANTE.equals(roleId)) {
            return 1;
        }
        if (WatheRoleIds.CIVILIAN.equals(roleId)) {
            return 2;
        }
        return 3;
    }

    private static int countFactionSeats(Map<UUID, Identifier> assignments, StrawFaction faction) {
        int count = 0;
        for (Identifier roleId : assignments.values()) {
            if (factionFor(roleId) == faction) {
                count++;
            }
        }
        return count;
    }

    private static int countGoodSeats(Map<UUID, Identifier> assignments) {
        int count = 0;
        for (Identifier roleId : assignments.values()) {
            if (WatheRoleIds.CIVILIAN.equals(roleId) || WatheRoleIds.VIGILANTE.equals(roleId)
                    || factionFor(roleId) == StrawFaction.GOOD) {
                count++;
            }
        }
        return count;
    }

    private static StrawFaction factionFor(Identifier roleId) {
        if (WatheRoleIds.KILLER.equals(roleId)) {
            return StrawFaction.KILLER;
        }
        if (WatheRoleIds.CIVILIAN.equals(roleId) || WatheRoleIds.VIGILANTE.equals(roleId)) {
            return StrawFaction.GOOD;
        }
        return NoellesRoleCatalog.find(roleId)
                .map(NoellesRoleCatalog.Entry::faction)
                .orElse(StrawFaction.NONE);
    }

    private static Role roleFor(Identifier roleId, Map<Identifier, Role> rolesById) {
        Role existingRole = rolesById.get(roleId);
        if (existingRole != null) {
            return existingRole;
        }
        return NoellesRoleCatalog.find(roleId)
                .map(NoellesRoleCatalog.Entry::watheRole)
                .orElseGet(() -> vanillaRoleFor(roleId));
    }

    private static Role vanillaRoleFor(Identifier roleId) {
        if (WatheRoleIds.KILLER.equals(roleId)) {
            return WatheRoles.KILLER;
        }
        if (WatheRoleIds.VIGILANTE.equals(roleId)) {
            return WatheRoles.VIGILANTE;
        }
        return WatheRoles.CIVILIAN;
    }
}
