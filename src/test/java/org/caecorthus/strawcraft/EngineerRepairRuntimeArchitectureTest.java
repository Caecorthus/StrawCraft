package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineerRepairRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/EngineerRepairToolRuntime.java");
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/EngineerRepairToolItem.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void engineerRepairToolUsesOfficialWatheDoorClassesAndFabricBlockUseCallback() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("net.fabricmc.fabric.api.event.player.UseBlockCallback"));
        assertTrue(runtime.contains("UseBlockCallback.EVENT.register"));
        assertTrue(runtime.contains("dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(player)"));
        assertTrue(runtime.contains("EngineerDoorRepairPolicy.shouldHandle"));
        assertTrue(runtime.contains("EngineerDoorRepairPolicy.actionFor"));
        assertTrue(runtime.contains("setBlasted(false)"));
        assertTrue(runtime.contains("setJammed(0)"));
        assertTrue(runtime.contains("setKeyName(\"\")"));
        assertTrue(runtime.contains("sync()"));
        assertTrue(runtime.contains("getItemCooldownManager().set(StrawCraftItems.REPAIR_TOOL"));
        assertTrue(item.contains("extends Item"));
    }

    @Test
    void engineerRepairRuntimeAvoidsSparkNoellesDoorPacketsClientUiAndWatheSourceEdits() throws IOException {
        for (Path path : mainJavaSources()) {
            String src = Files.readString(path, StandardCharsets.UTF_8);

            assertFalse(src.contains("DoorInteraction"), path + " must not use Spark/Parox DoorInteraction");
            assertFalse(src.contains("DoorStateChanged"), path + " must not use Spark door-state packets");
            assertFalse(src.contains("DoorHighlight"), path + " must not add door highlight packets");
            assertFalse(src.contains("import org.trainmurdermystery"), path + " must not import Spark Wathe runtime");
            assertFalse(src.contains("import org.noellesroles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(src.contains("XruiDD.NoellesRoles"), path + " must not depend on Spark NoellesRoles internals");
            assertFalse(src.contains("wathe-Parox"), path + " must not target Parox Wathe");
        }
    }

    private static List<Path> mainJavaSources() throws IOException {
        try (Stream<Path> sources = Files.walk(MAIN_ROOT)) {
            return sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }
}
