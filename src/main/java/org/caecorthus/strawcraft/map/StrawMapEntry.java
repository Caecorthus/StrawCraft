package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheMapEffects;
import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public record StrawMapEntry(
        Identifier id,
        Identifier dimensionId,
        Identifier gameModeId,
        Identifier mapEffectId,
        String displayName,
        Optional<String> description,
        int minPlayers,
        int maxPlayers
) {
    public static final Identifier DEFAULT_GAME_MODE = WatheGameModes.MURDER_ID;
    public static final Identifier DEFAULT_MAP_EFFECT = WatheMapEffects.GENERIC_ID;

    public StrawMapEntry {
        description = description == null ? Optional.empty() : description;
    }

    public boolean supportsGameMode(Identifier gameMode) {
        return gameModeId.equals(gameMode);
    }

    public boolean isEligible(int playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }

    public static Set<Identifier> defaultGameModes() {
        LinkedHashSet<Identifier> modes = new LinkedHashSet<>();
        modes.add(WatheGameModes.MURDER_ID);
        modes.add(WatheGameModes.DISCOVERY_ID);
        modes.add(WatheGameModes.LOOSE_ENDS_ID);
        return modes;
    }
}
