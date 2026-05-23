package org.caecorthus.strawcraft.map;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public Optional<StrawMapEntry> mapFor(Identifier dimensionId, Identifier gameModeId) {
        for (StrawMapEntry entry : maps.values()) {
            if (entry.dimensionId().equals(dimensionId) && entry.gameModeId().equals(gameModeId)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public Optional<StrawMapEntry> mapFor(Identifier dimensionId, Identifier gameModeId, Identifier mapEffectId) {
        Optional<StrawMapEntry> onlyMatch = Optional.empty();
        boolean hasMultipleMatches = false;
        for (StrawMapEntry entry : maps.values()) {
            if (!entry.dimensionId().equals(dimensionId) || !entry.gameModeId().equals(gameModeId)) {
                continue;
            }
            if (mapEffectId != null && entry.mapEffectId().equals(mapEffectId)) {
                return Optional.of(entry);
            }
            if (onlyMatch.isPresent()) {
                hasMultipleMatches = true;
            } else {
                onlyMatch = Optional.of(entry);
            }
        }
        // Legacy worlds may not have a current Wathe map effect yet; keep the old
        // two-key behavior only when no effect was provided and there is one possible Straw map.
        // 旧世界可能还没有当前 Wathe 地图效果；只有未提供效果且只有一个候选时才保留原来的二键兼容行为。
        if (mapEffectId == null && !hasMultipleMatches) {
            return onlyMatch;
        }
        return Optional.empty();
    }
}
