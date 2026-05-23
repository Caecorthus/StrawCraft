package org.caecorthus.strawcraft.map;

import com.google.gson.JsonParser;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheMapEffects;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.StrawCraft;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawRoomEnhancementTest {
    @AfterEach
    void clearRegistry() {
        StrawMapRegistry.getInstance().clear();
    }

    @Test
    void mapJsonCanDeclareRoomsWithCapacityKeysAndSpawns() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "display_name": "Room Train",
                  "game_modes": ["wathe:murder"],
                  "rooms": [
                    {
                      "id": "cabin_a",
                      "keyName": "Cabin A",
                      "capacity": 2,
                      "spawn": { "x": 10.5, "y": 64.0, "z": -3.5, "yaw": 90.0, "pitch": 0.0 }
                    }
                  ]
                }
                """;

        StrawMapEntry entry = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/room_train.json"),
                JsonParser.parseString(json)
        ).getFirst();

        assertEquals(1, entry.rooms().size());
        StrawRoomConfig room = entry.rooms().getFirst();
        assertEquals("cabin_a", room.id());
        assertEquals("Cabin A", room.keyName());
        assertEquals(2, room.capacity());
        assertEquals(List.of(new StrawRoomSpawnPoint(10.5, 64.0, -3.5, 90.0F, 0.0F)), room.spawns());
    }

    @Test
    void oldMapJsonStillUsesDefaultNamePlayerLimitAndNoRooms() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"]
                }
                """;

        StrawMapEntry entry = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/legacy_train.json"),
                JsonParser.parseString(json)
        ).getFirst();

        assertEquals("legacy_train", entry.displayName());
        assertEquals(Optional.empty(), entry.description());
        assertEquals(100, entry.maxPlayers());
        assertEquals(List.of(), entry.rooms());
    }

    @Test
    void playersAreAssignedByRoomCapacityInStableOrder() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID third = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID overflow = UUID.fromString("00000000-0000-0000-0000-000000000004");

        StrawRoomSpawnPoint cabinSpawn = new StrawRoomSpawnPoint(1.0, 2.0, 3.0, 0.0F, 0.0F);
        StrawRoomSpawnPoint lounge = new StrawRoomSpawnPoint(4.0, 5.0, 6.0, 180.0F, 0.0F);
        StrawRoomConfig cabinA = new StrawRoomConfig("cabin_a", "Cabin A", 2, List.of(cabinSpawn));
        StrawRoomConfig cabinB = new StrawRoomConfig("cabin_b", "Cabin B", 1, List.of(lounge));

        Map<UUID, StrawRoomAssignment> assignments = StrawRoomAllocator.assign(
                List.of(first, second, third, overflow),
                List.of(cabinA, cabinB)
        );

        assertEquals(new StrawRoomAssignment(cabinA, cabinSpawn), assignments.get(first));
        assertEquals(new StrawRoomAssignment(cabinA, cabinSpawn), assignments.get(second));
        assertEquals(new StrawRoomAssignment(cabinB, lounge), assignments.get(third));
        assertFalse(assignments.containsKey(overflow));
    }

    @Test
    void roomSpawnsCycleWhenRoomCapacityExceedsSpawnCount() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID third = UUID.fromString("00000000-0000-0000-0000-000000000003");
        StrawRoomSpawnPoint lower = new StrawRoomSpawnPoint(1.0, 2.0, 3.0, 0.0F, 0.0F);
        StrawRoomSpawnPoint upper = new StrawRoomSpawnPoint(1.5, 2.0, 3.5, 90.0F, 0.0F);
        StrawRoomConfig cabin = new StrawRoomConfig("cabin", "Cabin", 3, List.of(lower, upper));

        Map<UUID, StrawRoomAssignment> assignments = StrawRoomAllocator.assign(List.of(first, second, third), List.of(cabin));

        assertEquals(lower, assignments.get(first).spawn());
        assertEquals(upper, assignments.get(second).spawn());
        assertEquals(lower, assignments.get(third).spawn());
    }

    @Test
    void runtimeAdapterUsesOfficialWatheKeyLoreSemantics() throws Exception {
        String keyFactory = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/map/StrawRoomKeyFactory.java"),
                StandardCharsets.UTF_8
        );
        String bridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(keyFactory.contains("WatheItems.KEY"));
        assertTrue(keyFactory.contains("DataComponentTypes.LORE"));
        assertTrue(bridge.contains("GameEvents.ON_FINISH_INITIALIZE"));
        assertFalse(keyFactory.contains("MapEnhancementsWorldComponent"));
        assertFalse(keyFactory.contains("RoomData"));
    }

    @Test
    void roomRuntimeUsesCurrentMapEffectForSharedDimensionAndGameMode() {
        Identifier dimensionId = Identifier.of(StrawCraft.MOD_ID, "shared_train");
        StrawMapEntry generic = roomMap(
                "shared_generic",
                dimensionId,
                WatheMapEffects.GENERIC_ID,
                new StrawRoomConfig(
                        "generic_cabin",
                        "Generic Cabin",
                        1,
                        List.of(new StrawRoomSpawnPoint(1.0, 2.0, 3.0, 0.0F, 0.0F))
                )
        );
        StrawMapEntry sundown = roomMap(
                "shared_sundown",
                dimensionId,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID,
                new StrawRoomConfig(
                        "sundown_cabin",
                        "Sundown Cabin",
                        1,
                        List.of(new StrawRoomSpawnPoint(4.0, 5.0, 6.0, 90.0F, 0.0F))
                )
        );
        StrawMapRegistry.getInstance().register(generic);
        StrawMapRegistry.getInstance().register(sundown);

        Optional<StrawMapEntry> selectedMap = StrawRoomEnhancementAdapter.mapFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID
        );

        assertEquals(sundown, selectedMap.orElseThrow());
        assertEquals("sundown_cabin", selectedMap.orElseThrow().rooms().getFirst().id());
    }

    @Test
    void roomRuntimeDoesNotGuessWhenSharedMapEffectIsMissingOrDifferent() {
        Identifier dimensionId = Identifier.of(StrawCraft.MOD_ID, "shared_train");
        StrawMapRegistry.getInstance().register(roomMap(
                "shared_generic",
                dimensionId,
                WatheMapEffects.GENERIC_ID,
                new StrawRoomConfig(
                        "generic_cabin",
                        "Generic Cabin",
                        1,
                        List.of(new StrawRoomSpawnPoint(1.0, 2.0, 3.0, 0.0F, 0.0F))
                )
        ));
        StrawMapRegistry.getInstance().register(roomMap(
                "shared_sundown",
                dimensionId,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID,
                new StrawRoomConfig(
                        "sundown_cabin",
                        "Sundown Cabin",
                        1,
                        List.of(new StrawRoomSpawnPoint(4.0, 5.0, 6.0, 90.0F, 0.0F))
                )
        ));

        assertTrue(StrawRoomEnhancementAdapter.mapFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                null
        ).isEmpty());
        assertTrue(StrawRoomEnhancementAdapter.mapFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                Identifier.of(StrawCraft.MOD_ID, "unregistered_effect")
        ).isEmpty());
    }

    private static StrawMapEntry roomMap(
            String path,
            Identifier dimensionId,
            Identifier mapEffectId,
            StrawRoomConfig room
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
                List.of(room),
                StrawMapEnhancements.DEFAULT
        );
    }
}
