package org.caecorthus.strawcraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowingAxeFoundationArchitectureTest {
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");
    private static final Path MODEL = Path.of("src/main/resources/assets/strawcraft/models/item/throwing_axe.json");
    private static final Path ENGLISH = Path.of("src/main/resources/assets/strawcraft/lang/en_us.json");
    private static final Path CHINESE = Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json");

    @Test
    void throwingAxeRegistrationResourcesAndDeathReasonUseRealStrawCraftIds() throws IOException {
        assertEquals(StrawCraft.id("throwing_axe"), StrawCraftItems.THROWING_AXE_ID);
        assertEquals(StrawCraft.id("throwing_axe"), StrawDeathReasons.THROWING_AXE);

        JsonObject model = JsonParser.parseString(Files.readString(MODEL)).getAsJsonObject();
        assertEquals("item/handheld", model.get("parent").getAsString());
        assertEquals("minecraft:item/iron_axe", model.getAsJsonObject("textures").get("layer0").getAsString());

        JsonObject english = JsonParser.parseString(Files.readString(ENGLISH)).getAsJsonObject();
        JsonObject chinese = JsonParser.parseString(Files.readString(CHINESE)).getAsJsonObject();
        assertEquals("Throwing Axe", english.get("item.strawcraft.throwing_axe").getAsString());
        assertEquals("飞斧", chinese.get("item.strawcraft.throwing_axe").getAsString());
    }

    @Test
    void throwingAxeFoundationDoesNotExposeBanditAsRuntimeReadyOrShopRouted() throws IOException {
        assertFalse(NoellesRoleCatalog.find(StrawCraft.id("bandit")).orElseThrow().isRuntimeReady());
        assertFalse(NoellesRoleCatalog.runtimeSelectionDisabledIds().isEmpty());
        assertTrue(NoellesRoleCatalog.runtimeSelectionDisabledIds().contains(StrawCraft.id("bandit")));

        Set<String> shopFiles = Set.of(
                "KillerShopLoadout.java",
                "PlayerShopCatalog.java",
                "KillerShopCatalog.java"
        );
        for (String shopFile : shopFiles) {
            String source = Files.readString(MAIN_ROOT.resolve(shopFile), StandardCharsets.UTF_8);
            assertFalse(source.contains("THROWING_AXE"), shopFile + " must not route the foundation item into shops yet");
            assertFalse(source.contains("throwing_axe"), shopFile + " must not route the foundation item into shops yet");
        }
    }

    @Test
    void throwingAxeFoundationDoesNotImportForbiddenRuntimeSources() throws IOException {
        for (Path path : mainJavaSources()) {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            assertFalse(source.contains("import org.trainmurdermystery"), path + " must not import Spark Wathe runtime");
            assertFalse(source.contains("import org.noellesroles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(source.contains("XruiDD.NoellesRoles"), path + " must not depend on Spark NoellesRoles internals");
            assertFalse(source.contains("wathe-Parox"), path + " must not target Parox Wathe");
        }
    }

    private static Iterable<Path> mainJavaSources() throws IOException {
        try (Stream<Path> sources = Files.walk(MAIN_ROOT)) {
            return sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }
}
