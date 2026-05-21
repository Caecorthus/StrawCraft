package org.caecorthus.strawcraft.map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.doctor4t.wathe.api.WatheMapEffects;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.StrawCraft;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class StrawMapConfigReloader implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String MAPS_DATA_PATH = "maps";

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new StrawMapConfigReloader());
    }

    @Override
    public Identifier getFabricId() {
        return StrawCraft.id("map_configuration");
    }

    @Override
    public void reload(ResourceManager manager) {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        registry.clear();

        Map<Identifier, Resource> resources = manager.findResources(
                MAPS_DATA_PATH,
                id -> id.getNamespace().equals(StrawCraft.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> resource : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(
                    resource.getValue().getInputStream(),
                    StandardCharsets.UTF_8
            )) {
                registerMap(resource.getKey(), GSON.fromJson(reader, JsonElement.class), registry);
            } catch (Exception exception) {
                StrawCraft.LOGGER.error("Failed to load StrawCraft map config {}", resource.getKey(), exception);
            }
        }
        StrawCraft.LOGGER.info("Loaded {} StrawCraft voting maps", registry.maps().size());
    }

    private static void registerMap(Identifier resourceId, JsonElement json, StrawMapRegistry registry) {
        if (!json.isJsonObject()) {
            StrawCraft.LOGGER.error("Map config {} is not a JSON object", resourceId);
            return;
        }

        JsonObject object = json.getAsJsonObject();
        Identifier baseMapId = mapIdFromResource(resourceId);
        Identifier dimensionId = requiredIdentifier(object, "dimension", resourceId);
        if (dimensionId == null) {
            return;
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

        for (Identifier gameModeId : gameModes(object)) {
            // One physical map can be registered once per supported Wathe game mode,
            // giving each mode its own vote option while reusing the same dimension.
            // 同一张实体地图可以按支持的 Wathe 游戏模式分别注册，
            // 每个模式都有独立投票选项，但复用同一个维度。
            Identifier mapId = Identifier.of(
                    baseMapId.getNamespace(),
                    baseMapId.getPath() + "/" + gameModeId.getNamespace() + "/" + gameModeId.getPath()
            );
            registry.register(new StrawMapEntry(
                    mapId,
                    dimensionId,
                    gameModeId,
                    mapEffectId,
                    displayName,
                    description,
                    minPlayers,
                    maxPlayers
            ));
        }
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
}
