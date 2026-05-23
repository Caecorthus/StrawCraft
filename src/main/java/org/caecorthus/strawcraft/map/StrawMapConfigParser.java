package org.caecorthus.strawcraft.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.doctor4t.wathe.api.WatheMapEffects;
import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.StrawCraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class StrawMapConfigParser {
    private static final String MAPS_DATA_PATH = "maps";

    private StrawMapConfigParser() {
    }

    public static List<StrawMapEntry> parse(Identifier resourceId, JsonElement json) {
        if (!json.isJsonObject()) {
            StrawCraft.LOGGER.error("Map config {} is not a JSON object", resourceId);
            return List.of();
        }

        JsonObject object = json.getAsJsonObject();
        Identifier baseMapId = mapIdFromResource(resourceId);
        Identifier dimensionId = requiredIdentifier(object, "dimension", resourceId);
        if (dimensionId == null) {
            return List.of();
        }

        String displayName = object.has("display_name")
                ? object.get("display_name").getAsString()
                : baseMapId.getPath();
        Optional<String> description = object.has("description")
                ? Optional.of(object.get("description").getAsString())
                : Optional.empty();
        Identifier mapEffectId = optionalIdentifier(object, "map_effect", WatheMapEffects.GENERIC_ID);
        int minPlayers = object.has("min_players") ? object.get("min_players").getAsInt() : 0;
        int maxPlayers = object.has("max_players") ? object.get("max_players").getAsInt() : 100;
        JsonObject enhancementObject = enhancementObject(object);
        List<StrawRoomConfig> rooms = rooms(enhancementObject);
        StrawMapEnhancements enhancements = enhancements(object, enhancementObject);

        List<StrawMapEntry> entries = new ArrayList<>();
        for (Identifier gameModeId : gameModes(object)) {
            // One physical map can be registered once per supported Wathe game mode,
            // giving each mode its own vote option while reusing the same dimension.
            // 同一张实体地图可以按支持的 Wathe 游戏模式分别注册，
            // 每个模式都有独立投票选项，但复用同一个维度。
            Identifier mapId = Identifier.of(
                    baseMapId.getNamespace(),
                    baseMapId.getPath() + "/" + gameModeId.getNamespace() + "/" + gameModeId.getPath()
            );
            entries.add(new StrawMapEntry(
                    mapId,
                    dimensionId,
                    gameModeId,
                    mapEffectId,
                    displayName,
                    description,
                    minPlayers,
                    maxPlayers,
                    rooms,
                    enhancements
            ));
        }
        return entries;
    }

    private static Identifier mapIdFromResource(Identifier resourceId) {
        String path = resourceId.getPath();
        String name = path.substring(MAPS_DATA_PATH.length() + 1, path.length() - ".json".length());
        return Identifier.of(resourceId.getNamespace(), name);
    }

    private static Set<Identifier> gameModes(JsonObject object) {
        if (!object.has("game_modes") || !object.get("game_modes").isJsonArray()) {
            return StrawMapEntry.defaultGameModes();
        }
        LinkedHashSet<Identifier> modes = new LinkedHashSet<>();
        object.getAsJsonArray("game_modes").forEach(element -> {
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null) {
                modes.add(id);
            }
        });
        return modes.isEmpty() ? StrawMapEntry.defaultGameModes() : modes;
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
        // Older room drafts used a single "spawn" object; keep it as a one-point spawn list.
        // 早期房间草案使用单个 "spawn" 对象；这里兼容成只有一个点的出生点列表。
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

    private static Identifier requiredIdentifier(JsonObject object, String field, Identifier resourceId) {
        if (!object.has(field)) {
            StrawCraft.LOGGER.error("Map config {} is missing required field '{}'", resourceId, field);
            return null;
        }
        Identifier id = Identifier.tryParse(object.get(field).getAsString());
        if (id == null) {
            StrawCraft.LOGGER.error("Map config {} has invalid identifier in '{}'", resourceId, field);
        }
        return id;
    }

    private static Identifier optionalIdentifier(JsonObject object, String field, Identifier fallback) {
        if (!object.has(field)) {
            return fallback;
        }
        Identifier id = Identifier.tryParse(object.get(field).getAsString());
        return id == null ? fallback : id;
    }

    private static StrawMapEnhancements enhancements(JsonObject rootObject, JsonObject object) {
        return new StrawMapEnhancements(
                gravity(object),
                movement(object),
                jump(object),
                scenery(object),
                visibility(object),
                fog(object),
                cameraShake(object),
                ambience(object),
                interactionBlacklist(rootObject, object)
        );
    }

    private static JsonObject enhancementObject(JsonObject object) {
        if (object.has("enhancements") && object.get("enhancements").isJsonObject()) {
            return object.getAsJsonObject("enhancements");
        }
        return object;
    }

    private static StrawMapEnhancements.Gravity gravity(JsonObject object) {
        if (!object.has("gravity") || !object.get("gravity").isJsonObject()) {
            return StrawMapEnhancements.DEFAULT_GRAVITY;
        }
        JsonObject gravity = object.getAsJsonObject("gravity");
        return new StrawMapEnhancements.Gravity(floatOrDefault(gravity, "gravity_multiplier", 1.0F));
    }

    private static StrawMapEnhancements.Movement movement(JsonObject object) {
        if (!object.has("movement") || !object.get("movement").isJsonObject()) {
            return StrawMapEnhancements.DEFAULT_MOVEMENT;
        }
        JsonObject movement = object.getAsJsonObject("movement");
        return new StrawMapEnhancements.Movement(
                floatOrDefault(movement, "walk_speed_multiplier", 1.0F),
                floatOrDefault(movement, "sprint_speed_multiplier", 1.0F)
        );
    }

    private static StrawMapEnhancements.Jump jump(JsonObject object) {
        if (!object.has("jump") || !object.get("jump").isJsonObject()) {
            return StrawMapEnhancements.DEFAULT_JUMP;
        }
        JsonObject jump = object.getAsJsonObject("jump");
        return new StrawMapEnhancements.Jump(
                !jump.has("allowed") || jump.get("allowed").getAsBoolean(),
                floatOrDefault(jump, "stamina_cost", 0.0F)
        );
    }

    private static Optional<StrawMapEnhancements.Scenery> scenery(JsonObject object) {
        if (!object.has("scenery") || !object.get("scenery").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject scenery = object.getAsJsonObject("scenery");
        return Optional.of(new StrawMapEnhancements.Scenery(
                intOrDefault(scenery, "height_offset", 116),
                intOrDefault(scenery, "min_x", -208),
                intOrDefault(scenery, "max_x", 303),
                intOrDefault(scenery, "min_z", -896),
                intOrDefault(scenery, "max_z", -177)
        ));
    }

    private static Optional<StrawMapEnhancements.Visibility> visibility(JsonObject object) {
        if (!object.has("visibility") || !object.get("visibility").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject visibility = object.getAsJsonObject("visibility");
        return Optional.of(new StrawMapEnhancements.Visibility(
                intOrDefault(visibility, "day", 400),
                intOrDefault(visibility, "night", 200),
                intOrDefault(visibility, "sundown", 300)
        ));
    }

    private static Optional<StrawMapEnhancements.Fog> fog(JsonObject object) {
        if (!object.has("fog") || !object.get("fog").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject fog = object.getAsJsonObject("fog");
        return Optional.of(new StrawMapEnhancements.Fog(
                floatOrDefault(fog, "start", 32.0F),
                floatOrDefault(fog, "end_moving", 96.0F),
                floatOrDefault(fog, "end_stationary", 64.0F),
                colorOrDefault(fog, "night_color", 0x0D0D14)
        ));
    }

    private static Optional<StrawMapEnhancements.CameraShake> cameraShake(JsonObject object) {
        if (!object.has("camera_shake") || !object.get("camera_shake").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject cameraShake = object.getAsJsonObject("camera_shake");
        return Optional.of(new StrawMapEnhancements.CameraShake(
                !cameraShake.has("enabled") || cameraShake.get("enabled").getAsBoolean(),
                floatOrDefault(cameraShake, "amplitude_indoor", 0.002F),
                floatOrDefault(cameraShake, "amplitude_outdoor", 0.006F),
                floatOrDefault(cameraShake, "strength_indoor", 0.04F),
                floatOrDefault(cameraShake, "strength_outdoor", 0.08F)
        ));
    }

    private static Optional<StrawMapEnhancements.Ambience> ambience(JsonObject object) {
        if (!object.has("ambience") || !object.get("ambience").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject ambience = object.getAsJsonObject("ambience");
        return Optional.of(new StrawMapEnhancements.Ambience(
                !ambience.has("require_train_moving") || ambience.get("require_train_moving").getAsBoolean(),
                optionalString(ambience, "inside_sound"),
                optionalString(ambience, "outside_sound")
        ));
    }

    private static StrawMapEnhancements.InteractionBlacklist interactionBlacklist(JsonObject rootObject, JsonObject object) {
        if (object.has("interaction_blacklist") && object.get("interaction_blacklist").isJsonObject()) {
            return interactionBlacklist(object);
        }
        if (rootObject != object && rootObject.has("interaction_blacklist")
                && rootObject.get("interaction_blacklist").isJsonObject()) {
            return interactionBlacklist(rootObject);
        }
        return StrawMapEnhancements.DEFAULT_INTERACTION_BLACKLIST;
    }

    private static StrawMapEnhancements.InteractionBlacklist interactionBlacklist(JsonObject object) {
        if (!object.has("interaction_blacklist") || !object.get("interaction_blacklist").isJsonObject()) {
            return StrawMapEnhancements.DEFAULT_INTERACTION_BLACKLIST;
        }
        JsonObject blacklist = object.getAsJsonObject("interaction_blacklist");
        return new StrawMapEnhancements.InteractionBlacklist(
                identifiers(blacklist, "blocks"),
                blockTags(blacklist, "block_tags")
        );
    }

    private static Set<Identifier> identifiers(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonArray()) {
            return Set.of();
        }
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>();
        object.getAsJsonArray(field).forEach(element -> {
            if (!element.isJsonPrimitive()) {
                return;
            }
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
        });
        return ids;
    }

    private static Set<TagKey<Block>> blockTags(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonArray()) {
            return Set.of();
        }
        LinkedHashSet<TagKey<Block>> tags = new LinkedHashSet<>();
        object.getAsJsonArray(field).forEach(element -> {
            if (!element.isJsonPrimitive()) {
                return;
            }
            String raw = element.getAsString();
            String value = raw.startsWith("#") ? raw.substring(1) : raw;
            Identifier id = Identifier.tryParse(value);
            if (id != null) {
                tags.add(StrawMapEnhancements.InteractionBlacklist.blockTag(id));
            }
        });
        return tags;
    }

    private static double doubleOrZero(JsonObject object, String field) {
        return object.has(field) ? object.get(field).getAsDouble() : 0.0;
    }

    private static float floatOrZero(JsonObject object, String field) {
        return object.has(field) ? object.get(field).getAsFloat() : 0.0F;
    }

    private static int intOrDefault(JsonObject object, String field, int fallback) {
        return object.has(field) ? object.get(field).getAsInt() : fallback;
    }

    private static float floatOrDefault(JsonObject object, String field, float fallback) {
        return object.has(field) ? object.get(field).getAsFloat() : fallback;
    }

    private static Optional<String> optionalString(JsonObject object, String field) {
        if (!object.has(field)) {
            return Optional.empty();
        }
        String value = object.get(field).getAsString();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
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

    private static int colorOrDefault(JsonObject object, String field, int fallback) {
        if (!object.has(field)) {
            return fallback;
        }
        JsonElement element = object.get(field);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt();
        }
        String value = element.getAsString().trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.isEmpty() || value.length() > 8 || !value.matches("[0-9a-fA-F]+")) {
            return fallback;
        }
        return (int) Long.parseLong(value, 16);
    }
}
