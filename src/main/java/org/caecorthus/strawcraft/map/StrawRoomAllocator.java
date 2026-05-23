package org.caecorthus.strawcraft.map;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StrawRoomAllocator {
    private StrawRoomAllocator() {
    }

    public static Map<UUID, StrawRoomAssignment> assign(List<UUID> playerIds, List<StrawRoomConfig> rooms) {
        LinkedHashMap<UUID, StrawRoomAssignment> assignments = new LinkedHashMap<>();
        int playerIndex = 0;
        for (StrawRoomConfig room : rooms) {
            if (room.spawns().isEmpty()) {
                continue;
            }
            for (int slot = 0; slot < room.capacity() && playerIndex < playerIds.size(); slot++) {
                // Capacity is room-level; spawn points cycle inside a room when multiple players share it.
                // capacity 属于房间本身；同一房间住多人时，在该房间的出生点之间循环。
                assignments.put(playerIds.get(playerIndex), new StrawRoomAssignment(
                        room,
                        room.spawns().get(slot % room.spawns().size())
                ));
                playerIndex++;
            }
        }
        return assignments;
    }
}
