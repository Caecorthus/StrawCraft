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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawMapEnhancementsTest {
    @AfterEach
    void clearRegistry() {
        StrawMapRegistry.getInstance().clear();
    }

    @Test
    void parserAcceptsTmmStyleSafePlayerEnhancementFields() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"],
                  "enhancements": {
                    "rooms": [
                      {
                        "id": "suite",
                        "name": "Suite A",
                        "max_players": 2,
                        "spawn_points": [
                          { "x": 11.0, "y": 70.0, "z": -4.0, "yaw": 45.0, "pitch": 1.0 }
                        ]
                      }
                    ],
                    "gravity": { "gravity_multiplier": 0.65 },
                    "movement": { "walk_speed_multiplier": 0.8, "sprint_speed_multiplier": 1.15 },
                    "jump": { "allowed": false, "stamina_cost": 3.5 }
                  }
                }
                """;

        StrawMapEntry entry = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/enhanced_train.json"),
                JsonParser.parseString(json)
        ).getFirst();

        StrawMapEnhancements enhancements = entry.enhancements();
        StrawRoomConfig room = entry.rooms().getFirst();
        assertEquals("Suite A", room.keyName());
        assertEquals(2, room.capacity());
        assertEquals(List.of(new StrawRoomSpawnPoint(11.0, 70.0, -4.0, 45.0F, 1.0F)), room.spawns());
        assertEquals(0.65F, enhancements.gravity().gravityMultiplier());
        assertEquals(0.8F, enhancements.movement().walkSpeedMultiplier());
        assertEquals(1.15F, enhancements.movement().sprintSpeedMultiplier());
        assertFalse(enhancements.jump().allowed());
        assertEquals(3.5F, enhancements.jump().staminaCost());
    }

    @Test
    void oldMapJsonGetsNoOpEnhancementDefaults() {
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

        assertEquals(StrawMapEnhancements.DEFAULT, entry.enhancements());
        assertFalse(entry.enhancements().hasServerRuntimeEnhancements());
        assertEquals(1.0F, entry.enhancements().gravity().gravityMultiplier());
        assertEquals(1.0F, entry.enhancements().movement().walkSpeedMultiplier());
        assertEquals(1.0F, entry.enhancements().movement().sprintSpeedMultiplier());
        assertTrue(entry.enhancements().jump().allowed());
        assertFalse(entry.enhancements().changesGravity());
        assertFalse(entry.enhancements().changesMovement());
        assertFalse(entry.enhancements().changesJump());
        assertTrue(entry.enhancements().interactionBlacklist().isEmpty());
    }

    @Test
    void parserAcceptsInteractionBlacklistFromNestedEnhancements() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"],
                  "enhancements": {
                    "interaction_blacklist": {
                      "blocks": ["minecraft:barrel"],
                      "block_tags": ["minecraft:doors"]
                    }
                  }
                }
                """;

        StrawMapEnhancements.InteractionBlacklist blacklist = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/blacklist_train.json"),
                JsonParser.parseString(json)
        ).getFirst().enhancements().interactionBlacklist();

        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "barrel"), tag -> false));
        assertFalse(blacklist.blocksInteraction(Identifier.of("minecraft", "chest"), tag -> false));
        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "oak_door"),
                tag -> tag.id().equals(Identifier.of("minecraft", "doors"))));
    }

    @Test
    void parserAcceptsInteractionBlacklistFromTopLevelAndIgnoresMalformedEntries() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"],
                  "interaction_blacklist": {
                    "blocks": ["minecraft:crafting_table", "not valid"],
                    "block_tags": ["#minecraft:trapdoors", "bad tag"]
                  }
                }
                """;

        StrawMapEnhancements.InteractionBlacklist blacklist = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/top_level_blacklist.json"),
                JsonParser.parseString(json)
        ).getFirst().enhancements().interactionBlacklist();

        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "crafting_table"), tag -> false));
        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "stone"),
                tag -> tag.id().equals(Identifier.of("minecraft", "trapdoors"))));
        assertFalse(blacklist.blocksInteraction(Identifier.of("minecraft", "stone"), tag -> false));
    }

    @Test
    void parserAcceptsTopLevelInteractionBlacklistWhenEnhancementsObjectHasOtherFields() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"],
                  "interaction_blacklist": {
                    "blocks": ["minecraft:crafting_table"],
                    "block_tags": ["minecraft:trapdoors"]
                  },
                  "enhancements": {
                    "gravity": { "gravity_multiplier": 0.8 },
                    "movement": { "walk_speed_multiplier": 0.9, "sprint_speed_multiplier": 1.1 }
                  }
                }
                """;

        StrawMapEnhancements enhancements = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/mixed_blacklist_train.json"),
                JsonParser.parseString(json)
        ).getFirst().enhancements();
        StrawMapEnhancements.InteractionBlacklist blacklist = enhancements.interactionBlacklist();

        assertEquals(0.8F, enhancements.gravity().gravityMultiplier());
        assertEquals(0.9F, enhancements.movement().walkSpeedMultiplier());
        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "crafting_table"), tag -> false));
        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "stone"),
                tag -> tag.id().equals(Identifier.of("minecraft", "trapdoors"))));
    }

    @Test
    void parserUsesNestedInteractionBlacklistWhenBothLocationsDefineOne() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"],
                  "interaction_blacklist": {
                    "blocks": ["minecraft:crafting_table"]
                  },
                  "enhancements": {
                    "interaction_blacklist": {
                      "blocks": ["minecraft:barrel"]
                    }
                  }
                }
                """;

        StrawMapEnhancements.InteractionBlacklist blacklist = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/duplicate_blacklist_train.json"),
                JsonParser.parseString(json)
        ).getFirst().enhancements().interactionBlacklist();

        assertTrue(blacklist.blocksInteraction(Identifier.of("minecraft", "barrel"), tag -> false));
        assertFalse(blacklist.blocksInteraction(Identifier.of("minecraft", "crafting_table"), tag -> false));
    }

    @Test
    void explicitMapFieldsAreRequiredBeforeServerRuntimeChangesPlayerPhysics() {
        StrawMapEnhancements defaults = StrawMapEnhancements.DEFAULT;
        StrawMapEnhancements explicitGravity = new StrawMapEnhancements(
                new StrawMapEnhancements.Gravity(0.7F),
                StrawMapEnhancements.DEFAULT_MOVEMENT,
                StrawMapEnhancements.DEFAULT_JUMP,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        StrawMapEnhancements explicitMovement = new StrawMapEnhancements(
                StrawMapEnhancements.DEFAULT_GRAVITY,
                new StrawMapEnhancements.Movement(0.9F, 1.1F),
                StrawMapEnhancements.DEFAULT_JUMP,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        StrawMapEnhancements explicitJump = new StrawMapEnhancements(
                StrawMapEnhancements.DEFAULT_GRAVITY,
                StrawMapEnhancements.DEFAULT_MOVEMENT,
                new StrawMapEnhancements.Jump(false, 0.0F),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        StrawMapEnhancements staminaOnly = new StrawMapEnhancements(
                StrawMapEnhancements.DEFAULT_GRAVITY,
                StrawMapEnhancements.DEFAULT_MOVEMENT,
                new StrawMapEnhancements.Jump(true, 4.0F),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        assertFalse(defaults.hasServerRuntimeEnhancements());
        assertTrue(explicitGravity.changesGravity());
        assertFalse(explicitGravity.changesMovement());
        assertFalse(explicitGravity.changesJump());
        assertTrue(explicitMovement.changesMovement());
        assertTrue(explicitJump.changesJump());
        assertFalse(staminaOnly.hasServerRuntimeEnhancements());
    }

    @Test
    void parserModelsClientVisualAndAmbienceFieldsWithoutRuntimeEnablingThem() {
        String json = """
                {
                  "dimension": "minecraft:overworld",
                  "game_modes": ["wathe:murder"],
                  "scenery": { "height_offset": 120, "min_x": -10, "max_x": 10, "min_z": -20, "max_z": 20 },
                  "visibility": { "day": 400, "night": 160, "sundown": 260 },
                  "fog": { "start": 12.5, "end_moving": 60.0, "end_stationary": 32.0, "night_color": "#0D0D14" },
                  "camera_shake": { "enabled": true, "amplitude_indoor": 0.002, "amplitude_outdoor": 0.006, "strength_indoor": 0.04, "strength_outdoor": 0.08 },
                  "ambience": { "require_train_moving": false, "inside_sound": "", "outside_sound": "wathe:ambient.ship.outside" }
                }
                """;

        StrawMapEnhancements enhancements = StrawMapConfigParser.parse(
                Identifier.of(StrawCraft.MOD_ID, "maps/client_train.json"),
                JsonParser.parseString(json)
        ).getFirst().enhancements();

        assertEquals(Optional.of(new StrawMapEnhancements.Scenery(120, -10, 10, -20, 20)), enhancements.scenery());
        assertEquals(Optional.of(new StrawMapEnhancements.Visibility(400, 160, 260)), enhancements.visibility());
        assertEquals(Optional.of(new StrawMapEnhancements.Fog(12.5F, 60.0F, 32.0F, 0x0D0D14)), enhancements.fog());
        assertEquals(Optional.empty(), enhancements.ambience().orElseThrow().insideSound());
        assertEquals(Optional.of("wathe:ambient.ship.outside"), enhancements.ambience().orElseThrow().outsideSound());
        assertFalse(enhancements.hasServerRuntimeEnhancements());
    }

    @Test
    void runtimeDoesNotPullInTmmOnlyEnhancementState() throws Exception {
        String runtime = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/map/StrawPlayerEnhancementAdapter.java"),
                StandardCharsets.UTF_8
        );
        String mixinConfig = Files.readString(Path.of("src/main/resources/strawcraft.mixins.json"), StandardCharsets.UTF_8);

        assertTrue(runtime.contains("GENERIC_GRAVITY"));
        assertTrue(runtime.contains("hasServerRuntimeEnhancements"));
        assertTrue(mixinConfig.contains("\"LivingEntityMixin\""));
        String interactionRuntime = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/map/StrawInteractionBlacklistAdapter.java"),
                StandardCharsets.UTF_8
        );
        String strawCraft = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"), StandardCharsets.UTF_8);

        assertFalse(runtime.contains("MapEnhancementsWorldComponent"));
        assertFalse(runtime.contains("RoomData"));
        assertTrue(interactionRuntime.contains("UseBlockCallback.EVENT.register"));
        assertTrue(interactionRuntime.contains("ActionResult.FAIL"));
        assertTrue(interactionRuntime.contains("isRunning()"));
        assertTrue(interactionRuntime.contains("getRoles().containsKey"));
        assertTrue(strawCraft.contains("StrawInteractionBlacklistAdapter.register()"));
        assertFalse(interactionRuntime.contains("MapEnhancementsWorldComponent"));
        assertFalse(interactionRuntime.contains("RoomData"));
    }

    @Test
    void jumpStaminaCostIsModeledButNotConsumedWithoutTmmStaminaComponent() {
        StrawMapEnhancements.Jump jump = new StrawMapEnhancements.Jump(true, 4.0F);

        assertTrue(jump.allowed());
        assertEquals(4.0F, jump.staminaCost());
    }

    @Test
    void playerRuntimeEnhancementsUseCurrentMapEffectForSharedDimensionAndGameMode() {
        Identifier dimensionId = Identifier.of(StrawCraft.MOD_ID, "shared_train");
        StrawMapEntry generic = enhancedMap(
                "shared_generic",
                dimensionId,
                WatheMapEffects.GENERIC_ID,
                new StrawMapEnhancements.Movement(0.5F, 0.6F)
        );
        StrawMapEntry sundown = enhancedMap(
                "shared_sundown",
                dimensionId,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID,
                new StrawMapEnhancements.Movement(0.8F, 1.2F)
        );
        StrawMapRegistry.getInstance().register(generic);
        StrawMapRegistry.getInstance().register(sundown);

        Optional<StrawMapEnhancements> selectedEnhancements = StrawPlayerEnhancementAdapter.enhancementsFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID
        );

        assertEquals(sundown.enhancements(), selectedEnhancements.orElseThrow());
        assertEquals(1.2F, selectedEnhancements.orElseThrow().movement().sprintSpeedMultiplier());
    }

    @Test
    void playerRuntimeEnhancementsDoNotGuessWhenSharedMapEffectIsMissingOrDifferent() {
        Identifier dimensionId = Identifier.of(StrawCraft.MOD_ID, "shared_train");
        StrawMapRegistry.getInstance().register(enhancedMap(
                "shared_generic",
                dimensionId,
                WatheMapEffects.GENERIC_ID,
                new StrawMapEnhancements.Movement(0.5F, 0.6F)
        ));
        StrawMapRegistry.getInstance().register(enhancedMap(
                "shared_sundown",
                dimensionId,
                WatheMapEffects.HARPY_EXPRESS_SUNDOWN_ID,
                new StrawMapEnhancements.Movement(0.8F, 1.2F)
        ));

        assertTrue(StrawPlayerEnhancementAdapter.enhancementsFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                null
        ).isEmpty());
        assertTrue(StrawPlayerEnhancementAdapter.enhancementsFor(
                dimensionId,
                WatheGameModes.MURDER_ID,
                Identifier.of(StrawCraft.MOD_ID, "unregistered_effect")
        ).isEmpty());
    }

    private static StrawMapEntry enhancedMap(
            String path,
            Identifier dimensionId,
            Identifier mapEffectId,
            StrawMapEnhancements.Movement movement
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
                        movement,
                        StrawMapEnhancements.DEFAULT_JUMP,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
    }
}
