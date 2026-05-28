package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScavengerHiddenBodyState {
    private final Map<Identifier, Set<UUID>> hiddenBodiesByWorld = new ConcurrentHashMap<>();

    public void recordHiddenBody(Identifier worldId, UUID bodyPlayerUuid) {
        hiddenBodiesByWorld
                .computeIfAbsent(worldId, ignored -> ConcurrentHashMap.newKeySet())
                .add(bodyPlayerUuid);
    }

    public boolean isHiddenBody(Identifier worldId, UUID bodyPlayerUuid) {
        return hiddenBodiesByWorld.getOrDefault(worldId, Set.of()).contains(bodyPlayerUuid);
    }

    public void clearHiddenBody(Identifier worldId, UUID bodyPlayerUuid) {
        Set<UUID> hiddenBodies = hiddenBodiesByWorld.get(worldId);
        if (hiddenBodies == null) {
            return;
        }

        hiddenBodies.remove(bodyPlayerUuid);
        if (hiddenBodies.isEmpty()) {
            hiddenBodiesByWorld.remove(worldId);
        }
    }

    public void clearWorld(Identifier worldId) {
        hiddenBodiesByWorld.remove(worldId);
    }

    public void clearAll() {
        hiddenBodiesByWorld.clear();
    }
}
