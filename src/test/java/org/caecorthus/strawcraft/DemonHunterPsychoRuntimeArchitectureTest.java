package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemonHunterPsychoRuntimeArchitectureTest {
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");
    private static final Path MIXIN = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerPsychoComponentMixin.java");
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/DemonHunterPsychoRuntime.java");
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/DemonHunterPistolItem.java");

    @Test
    void officialWathePsychoComponentExposesStartAndStopHooksUsedByRuntime() throws NoSuchMethodException {
        Method start = PlayerPsychoComponent.class.getDeclaredMethod("startPsycho");
        Method stop = PlayerPsychoComponent.class.getDeclaredMethod("stopPsycho");

        assertEquals(boolean.class, start.getReturnType());
        assertEquals(void.class, stop.getReturnType());
        assertFalse(hasMethod("startPsycho", boolean.class));
        assertFalse(hasMethod("stopPsycho", boolean.class));
    }

    @Test
    void demonHunterRuntimeIsWiredOnlyToLoudPsychoMixinPath() throws IOException {
        String mixin = Files.readString(MIXIN, StandardCharsets.UTF_8);
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(mixin.contains("DemonHunterPsychoRuntime.onLoudPsychoStarted(player)"));
        assertTrue(mixin.contains("DemonHunterPsychoRuntime.onLoudPsychoStopped(player)"));
        assertTrue(mixin.contains("if (!strawcraft$silentPsycho)"));
        assertTrue(runtime.contains("DemonHunterPsychoPolicy.onPsychoStarted"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(demonHunter)"));
        assertTrue(runtime.contains("trackDemonHunterFrenziedPlayer"));
        assertTrue(runtime.contains("untrackDemonHunterFrenziedPlayer"));
        assertTrue(runtime.contains("removeEmptyDemonHunterPistols"));
    }

    @Test
    void demonHunterPistolRegistrationResourcesAndDeathReasonUseRealStrawCraftIds() throws IOException {
        assertEquals(StrawCraft.id("demon_hunter_pistol"), StrawCraftItems.DEMON_HUNTER_PISTOL_ID);
        assertEquals(StrawCraft.id("demon_hunter_shot"), StrawDeathReasons.DEMON_HUNTER_SHOT);
        assertDoesNotThrow(() -> Files.readString(Path.of("src/main/resources/assets/strawcraft/models/item/demon_hunter_pistol.json")));
        assertTrue(Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/en_us.json"))
                .contains("\"item.strawcraft.demon_hunter_pistol\""));
        assertTrue(Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json"))
                .contains("\"item.strawcraft.demon_hunter_pistol\""));

        String item = Files.readString(ITEM, StandardCharsets.UTF_8);
        assertTrue(item.contains("DemonHunterPsychoPolicy.evaluateShot"));
        assertTrue(item.contains("GameFunctions.killPlayer(target, true, shooter, StrawDeathReasons.DEMON_HUNTER_SHOT)"));
    }

    @Test
    void demonHunterSliceDocumentsThatJesterExceptionIsDeferred() throws IOException {
        String policy = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/DemonHunterPsychoPolicy.java"), StandardCharsets.UTF_8);
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertFalse(policy.toLowerCase(java.util.Locale.ROOT).contains("jester"));
        assertFalse(runtime.toLowerCase(java.util.Locale.ROOT).contains("jester"));
        assertFalse(item.toLowerCase(java.util.Locale.ROOT).contains("jester"));
    }

    @Test
    void demonHunterRuntimeDoesNotImportSparkNoellesOrParoxRuntimeApis() throws IOException {
        for (Path path : mainJavaSources()) {
            String src = Files.readString(path, StandardCharsets.UTF_8);
            assertFalse(src.contains("import org.trainmurdermystery"), path + " must not import Spark Wathe runtime");
            assertFalse(src.contains("import org.noellesroles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(src.contains("XruiDD.NoellesRoles"), path + " must not depend on Spark NoellesRoles internals");
            assertFalse(src.contains("wathe-Parox"), path + " must not target Parox Wathe");
        }
    }

    private static boolean hasMethod(String name, Class<?>... parameterTypes) {
        return Arrays.stream(PlayerPsychoComponent.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals(name)
                        && Arrays.equals(method.getParameterTypes(), parameterTypes));
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
