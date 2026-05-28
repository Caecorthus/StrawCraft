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

class BartenderDefenseVialRuntimeArchitectureTest {
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/BartenderDefenseVialItem.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void defenseVialRuntimeUsesOfficialWathePoisonComponentResetForSelfOnly() throws IOException {
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(item.contains("dev.doctor4t.wathe.cca.PlayerPoisonComponent"));
        assertTrue(item.contains("PlayerPoisonComponent.KEY.get(player)"));
        assertTrue(item.contains("poisonComponent.reset()"));
        assertFalse(item.contains("findNearbyPoisonedPlayer"));
    }

    @Test
    void defenseVialRuntimeDoesNotImportSparkNoellesOrParoxRuntimeApis() throws IOException {
        for (Path path : mainJavaSources()) {
            String src = Files.readString(path, StandardCharsets.UTF_8);
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
