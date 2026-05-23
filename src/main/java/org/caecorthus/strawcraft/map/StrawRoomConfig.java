package org.caecorthus.strawcraft.map;

public record StrawRoomConfig(
        String id,
        String keyName,
        int capacity,
        java.util.List<StrawRoomSpawnPoint> spawns
) {
    public StrawRoomConfig {
        if (capacity < 0) {
            capacity = 0;
        }
        spawns = spawns == null ? java.util.List.of() : java.util.List.copyOf(spawns);
    }
}
