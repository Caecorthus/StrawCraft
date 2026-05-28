package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToxicologistAntidoteRuntimeArchitectureTest {
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/ToxicologistAntidoteItem.java");
    private static final Path LOADOUTS = Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void officialWathePoisonComponentExposesObservableTicksAndReset() throws ReflectiveOperationException {
        Field key = PlayerPoisonComponent.class.getField("KEY");
        Field poisonTicks = PlayerPoisonComponent.class.getField("poisonTicks");
        Method reset = PlayerPoisonComponent.class.getMethod("reset");

        assertNotNull(key);
        assertNotNull(poisonTicks);
        assertNotNull(reset);
    }

    @Test
    void antidoteRuntimeUsesOfficialWathePoisonComponentReset() throws IOException {
        String src = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(src.contains("dev.doctor4t.wathe.cca.PlayerPoisonComponent"));
        assertTrue(src.contains("PlayerPoisonComponent.KEY.get"));
        assertTrue(src.contains("poisonComponent.reset()"));
        assertFalse(src.contains("setPoisonTicks(-1"));
    }

    @Test
    void antidoteCooldownsUseCleanMinecraftItemCooldownInfrastructure() throws IOException {
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);
        String loadouts = Files.readString(LOADOUTS, StandardCharsets.UTF_8);

        assertTrue(item.contains("getItemCooldownManager().set(StrawCraftItems.ANTIDOTE"));
        assertTrue(loadouts.contains("ToxicologistAntidoteItem.applyInitialAssignmentCooldown"));
    }

    @Test
    void antidoteConsumesStackOnlyAfterSuccessfulServerCureForNonCreativePlayers() throws IOException {
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(item.contains("!world.isClient() && user instanceof ServerPlayerEntity player && curePoison(player)"));
        assertTrue(item.contains("!player.getAbilities().creativeMode"));
        assertTrue(item.contains("stack.decrement(1)"));
    }

    @Test
    void toxicologistRuntimeDoesNotImportSparkNoellesPoisonCopiesOrEditWathe() throws IOException {
        for (Path path : mainJavaSources()) {
            String src = Files.readString(path, StandardCharsets.UTF_8);
            assertFalse(src.contains("import org.trainmurdermystery"), path + " must not import Spark Wathe runtime");
            assertFalse(src.contains("import org.noellesroles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(src.contains("XruiDD.NoellesRoles"), path + " must not depend on Spark NoellesRoles internals");
            assertFalse(src.contains("PoisonUtils"), path + " must not copy official poison utility internals");
        }
    }

    @Test
    void itemRegistrationAndResourcesUseRealStrawCraftAntidoteId() {
        assertDoesNotThrow(() -> Files.readString(Path.of("src/main/resources/assets/strawcraft/models/item/antidote.json")));
        assertTrue(StrawCraft.id("antidote").equals(StrawCraftItems.ANTIDOTE_ID));
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
