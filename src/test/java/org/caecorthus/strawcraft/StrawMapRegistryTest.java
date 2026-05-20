package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheMapEffects;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.map.StrawMapEntry;
import org.caecorthus.strawcraft.map.StrawMapRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawMapRegistryTest {
    @AfterEach
    void clearRegistry() {
        StrawMapRegistry.getInstance().clear();
    }

    @Test
    void registryKeepsInsertionOrderAndFiltersByGameMode() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        StrawMapEntry murderMap = map("mansion", WatheGameModes.MURDER_ID, 4, 12);
        StrawMapEntry looseEndsMap = map("station", WatheGameModes.LOOSE_ENDS_ID, 2, 8);

        registry.register(murderMap);
        registry.register(looseEndsMap);

        assertEquals(murderMap, registry.mapsForGameMode(WatheGameModes.MURDER_ID).getFirst());
        assertEquals(looseEndsMap, registry.mapsForGameMode(WatheGameModes.LOOSE_ENDS_ID).getFirst());
    }

    @Test
    void eligibleMapsRespectPlayerLimits() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        StrawMapEntry small = map("small", WatheGameModes.MURDER_ID, 2, 6);
        StrawMapEntry large = map("large", WatheGameModes.MURDER_ID, 8, 20);

        registry.register(small);
        registry.register(large);

        assertEquals(small, registry.eligibleMapsForGameMode(WatheGameModes.MURDER_ID, 5).getFirst());
        assertEquals(large, registry.eligibleMapsForGameMode(WatheGameModes.MURDER_ID, 12).getFirst());
        assertTrue(registry.eligibleMapsForGameMode(WatheGameModes.MURDER_ID, 7).isEmpty());
    }

    private static StrawMapEntry map(String path, Identifier gameModeId, int minPlayers, int maxPlayers) {
        return new StrawMapEntry(
                Identifier.of(StrawCraft.MOD_ID, path),
                Identifier.of(StrawCraft.MOD_ID, path),
                gameModeId,
                WatheMapEffects.GENERIC_ID,
                path,
                Optional.empty(),
                minPlayers,
                maxPlayers
        );
    }
}
