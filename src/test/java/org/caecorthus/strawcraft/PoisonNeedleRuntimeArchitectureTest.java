package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoisonNeedleRuntimeArchitectureTest {
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/PoisonNeedleItem.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void officialWathePoisonComponentExposesMutablePoisonTicks() throws ReflectiveOperationException {
        Field key = PlayerPoisonComponent.class.getField("KEY");
        Field poisonTicks = PlayerPoisonComponent.class.getField("poisonTicks");

        assertNotNull(key);
        assertNotNull(poisonTicks);
        assertFalse(Modifier.isFinal(poisonTicks.getModifiers()));
    }

    @Test
    void poisonNeedleRuntimeUsesOfficialWathePoisonComponentState() throws IOException {
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(item.contains("dev.doctor4t.wathe.cca.PlayerPoisonComponent"));
        assertTrue(item.contains("PlayerPoisonComponent.KEY.get(target)"));
        assertTrue(item.contains("poison.poisonTicks = decision.poisonTicksAfterUse()"));
        assertTrue(item.contains("PlayerPoisonComponent.KEY.sync(target)"));
        assertFalse(item.contains("setPoisonTicks"));
    }

    @Test
    void itemRegistrationAndResourcesUseRealStrawCraftPoisonNeedleId() throws IOException {
        assertTrue(StrawCraft.id("poison_needle").equals(StrawCraftItems.POISON_NEEDLE_ID));
        assertDoesNotThrow(() -> Files.readString(Path.of("src/main/resources/assets/strawcraft/models/item/poison_needle.json")));
        assertTrue(Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/en_us.json"))
                .contains("\"item.strawcraft.poison_needle\""));
        assertTrue(Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json"))
                .contains("\"item.strawcraft.poison_needle\""));
    }

    @Test
    void poisonNeedleRuntimeDoesNotImportSparkNoellesOrParoxRuntimeApis() throws IOException {
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
