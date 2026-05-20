package org.caecorthus.strawcraft.map;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StrawMapRegistry {
    private static final StrawMapRegistry INSTANCE = new StrawMapRegistry();

    private final Map<Identifier, StrawMapEntry> maps = new LinkedHashMap<>();

    private StrawMapRegistry() {
    }

    public static StrawMapRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        maps.clear();
    }

    public void register(StrawMapEntry entry) {
        maps.put(entry.id(), entry);
    }

    public Map<Identifier, StrawMapEntry> maps() {
        return Collections.unmodifiableMap(maps);
    }

    public List<StrawMapEntry> mapsForGameMode(Identifier gameModeId) {
        List<StrawMapEntry> filtered = new ArrayList<>();
        for (StrawMapEntry entry : maps.values()) {
            if (entry.supportsGameMode(gameModeId)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public List<StrawMapEntry> eligibleMapsForGameMode(Identifier gameModeId, int playerCount) {
        List<StrawMapEntry> eligible = new ArrayList<>();
        for (StrawMapEntry entry : mapsForGameMode(gameModeId)) {
            if (entry.isEligible(playerCount)) {
                eligible.add(entry);
            }
        }
        return eligible;
    }
}
