package org.caecorthus.strawcraft.map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.StrawCraft;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class StrawMapConfigReloader implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new Gson();

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
                "maps",
                id -> id.getNamespace().equals(StrawCraft.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> resource : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(
                    resource.getValue().getInputStream(),
                    StandardCharsets.UTF_8
            )) {
                StrawMapConfigParser.parse(resource.getKey(), GSON.fromJson(reader, JsonElement.class))
                        .forEach(registry::register);
            } catch (Exception exception) {
                StrawCraft.LOGGER.error("Failed to load StrawCraft map config {}", resource.getKey(), exception);
            }
        }
        StrawCraft.LOGGER.info("Loaded {} StrawCraft voting maps", registry.maps().size());
    }
}
