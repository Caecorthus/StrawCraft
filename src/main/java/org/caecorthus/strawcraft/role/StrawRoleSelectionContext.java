package org.caecorthus.strawcraft.role;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record StrawRoleSelectionContext(
        List<UUID> players,
        int killerTargetCount,
        int neutralTargetCount,
        int witchTargetCount,
        int specialGoodTargetCount,
        Set<Identifier> disabledRoles,
        Map<UUID, Identifier> existingAssignments
) {
    public StrawRoleSelectionContext {
        players = List.copyOf(players);
        killerTargetCount = Math.max(0, killerTargetCount);
        neutralTargetCount = Math.max(0, neutralTargetCount);
        witchTargetCount = Math.max(0, witchTargetCount);
        specialGoodTargetCount = Math.max(0, specialGoodTargetCount);
        disabledRoles = Set.copyOf(disabledRoles);
        existingAssignments = Map.copyOf(existingAssignments);
    }

    int targetCountFor(StrawFaction faction) {
        return switch (faction) {
            case KILLER -> killerTargetCount;
            case NEUTRAL -> neutralTargetCount;
            case WITCH -> witchTargetCount;
            case GOOD -> specialGoodTargetCount;
            case NONE -> 0;
        };
    }
}
