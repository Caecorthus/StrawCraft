package org.caecorthus.strawcraft.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record MapDefinition(
        List<StrawRoomConfig> rooms
) {
    public static final MapDefinition EMPTY = new MapDefinition(List.of());

    public MapDefinition {
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
    }

    public static MapDefinition from(JsonObject rootObject, JsonObject enhancementObject) {
        JsonObject roomSource = roomSource(rootObject, enhancementObject);
        return new MapDefinition(rooms(roomSource));
    }

    private static JsonObject roomSource(JsonObject rootObject, JsonObject enhancementObject) {
        if (enhancementObject != rootObject
                && enhancementObject.has("rooms")
                && enhancementObject.get("rooms").isJsonArray()) {
            return enhancementObject;
        }
        return rootObject;
    }

    private static List<StrawRoomConfig> rooms(JsonObject object) {
        if (!object.has("rooms") || !object.get("rooms").isJsonArray()) {
            return List.of();
        }

        List<StrawRoomConfig> rooms = new ArrayList<>();
        object.getAsJsonArray("rooms").forEach(element -> {
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject room = element.getAsJsonObject();
            String id = room.has("id") ? room.get("id").getAsString() : "";
            String keyName = roomString(room, "keyName", "key_name", "displayName", "display_name", "name")
                    .orElse(id);
            int capacity = room.has("capacity")
                    ? room.get("capacity").getAsInt()
                    : room.has("max_players") ? room.get("max_players").getAsInt() : 0;
            rooms.add(new StrawRoomConfig(id, keyName, capacity, spawns(room)));
        });
        return rooms;
    }

    private static List<StrawRoomSpawnPoint> spawns(JsonObject room) {
        if (room.has("spawn_points") && room.get("spawn_points").isJsonArray()) {
            return spawnList(room.getAsJsonArray("spawn_points"));
        }
        if (room.has("spawns") && room.get("spawns").isJsonArray()) {
            return spawnList(room.getAsJsonArray("spawns"));
        }
        if (room.has("spawn") && room.get("spawn").isJsonObject()) {
            return List.of(spawn(room.getAsJsonObject("spawn")));
        }
        return List.of();
    }

    private static List<StrawRoomSpawnPoint> spawnList(Iterable<JsonElement> elements) {
        List<StrawRoomSpawnPoint> spawns = new ArrayList<>();
        elements.forEach(element -> {
            if (element.isJsonObject()) {
                spawns.add(spawn(element.getAsJsonObject()));
            }
        });
        return spawns;
    }

    private static StrawRoomSpawnPoint spawn(JsonObject spawn) {
        return new StrawRoomSpawnPoint(
                doubleOrZero(spawn, "x"),
                doubleOrZero(spawn, "y"),
                doubleOrZero(spawn, "z"),
                floatOrZero(spawn, "yaw"),
                floatOrZero(spawn, "pitch")
        );
    }

    private static double doubleOrZero(JsonObject object, String field) {
        return object.has(field) ? object.get(field).getAsDouble() : 0.0;
    }

    private static float floatOrZero(JsonObject object, String field) {
        return object.has(field) ? object.get(field).getAsFloat() : 0.0F;
    }

    private static Optional<String> roomString(JsonObject object, String... fields) {
        for (String field : fields) {
            if (object.has(field)) {
                String value = object.get(field).getAsString();
                if (!value.isEmpty()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }
}
