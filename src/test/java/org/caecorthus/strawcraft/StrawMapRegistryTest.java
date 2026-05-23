package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheMapEffects;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.map.StrawMapEntry;
import org.caecorthus.strawcraft.map.StrawMapEnhancements;
import org.caecorthus.strawcraft.map.StrawMapRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void activeMapResolutionRequiresMatchingDimensionAndGameMode() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        StrawMapEntry murderMap = map("mansion", WatheGameModes.MURDER_ID, 4, 12);
        StrawMapEntry looseEndsMap = map("station", WatheGameModes.LOOSE_ENDS_ID, 2, 8);

        registry.register(murderMap);
        registry.register(looseEndsMap);

        assertEquals(murderMap, registry.mapFor(
                Identifier.of(StrawCraft.MOD_ID, "mansion"),
                WatheGameModes.MURDER_ID
        ).orElseThrow());
        assertTrue(registry.mapFor(
                Identifier.of(StrawCraft.MOD_ID, "mansion"),
                WatheGameModes.LOOSE_ENDS_ID
        ).isEmpty());
    }

    @Test
    void activeMapResolutionPrefersCurrentMapEffectWhenDimensionAndGameModeAreShared() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        Identifier dimensionId = Identifier.of(StrawCraft.MOD_ID, "shared_train");
        Identifier selectedEffectId = WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID;
        StrawMapEntry firstRegistered = mapWithBlacklist(
                "shared_generic",
                dimensionId,
                WatheMapEffects.GENERIC_ID,
                Identifier.of("minecraft", "barrel")
        );
        StrawMapEntry selectedMap = mapWithBlacklist(
                "shared_sundown",
                dimensionId,
                selectedEffectId,
                Identifier.of("minecraft", "crafting_table")
        );

        registry.register(firstRegistered);
        registry.register(selectedMap);

        StrawMapEnhancements.InteractionBlacklist blacklist = registry.mapFor(
                        dimensionId,
                        WatheGameModes.MURDER_ID,
                        selectedEffectId
                )
                .orElseThrow()
                .enhancements()
                .interactionBlacklist();

        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "crafting_table"), tag -> false));
        assertFalse(blacklist.blocksInteraction(Identifier.of("minecraft", "barrel"), tag -> false));
    }

    @Test
    void activeMapResolutionFallsBackWhenCurrentMapEffectIsUnavailableForASingleCandidate() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        StrawMapEntry legacyMap = map("legacy_train", WatheGameModes.MURDER_ID, 2, 12);

        registry.register(legacyMap);

        assertEquals(legacyMap, registry.mapFor(
                Identifier.of(StrawCraft.MOD_ID, "legacy_train"),
                WatheGameModes.MURDER_ID,
                null
        ).orElseThrow());
    }

    @Test
    void activeMapResolutionDoesNotGuessWhenCurrentMapEffectDoesNotMatchSingleCandidate() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        StrawMapEntry legacyMap = map("legacy_train", WatheGameModes.MURDER_ID, 2, 12);

        registry.register(legacyMap);

        assertTrue(registry.mapFor(
                Identifier.of(StrawCraft.MOD_ID, "legacy_train"),
                WatheGameModes.MURDER_ID,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID
        ).isEmpty());
    }

    @Test
    void activeMapResolutionDoesNotGuessWhenMapEffectIsMissingForSharedDimensionAndGameMode() {
        StrawMapRegistry registry = StrawMapRegistry.getInstance();
        Identifier dimensionId = Identifier.of(StrawCraft.MOD_ID, "shared_train");

        registry.register(mapWithBlacklist(
                "shared_generic",
                dimensionId,
                WatheMapEffects.GENERIC_ID,
                Identifier.of("minecraft", "barrel")
        ));
        registry.register(mapWithBlacklist(
                "shared_sundown",
                dimensionId,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID,
                Identifier.of("minecraft", "crafting_table")
        ));

        assertTrue(registry.mapFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                null
        ).isEmpty());
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

    private static StrawMapEntry mapWithBlacklist(
            String path,
            Identifier dimensionId,
            Identifier mapEffectId,
            Identifier blockedBlock
    ) {
        return new StrawMapEntry(
                Identifier.of(StrawCraft.MOD_ID, path),
                dimensionId,
                WatheGameModes.MURDER_ID,
                mapEffectId,
                path,
                Optional.empty(),
                2,
                12,
                List.of(),
                new StrawMapEnhancements(
                        StrawMapEnhancements.DEFAULT_GRAVITY,
                        StrawMapEnhancements.DEFAULT_MOVEMENT,
                        StrawMapEnhancements.DEFAULT_JUMP,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        new StrawMapEnhancements.InteractionBlacklist(Set.of(blockedBlock), Set.of())
                )
        );
    }
}
