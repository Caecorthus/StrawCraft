package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleMeaningArchitectureTest {
    private static final Path STRAWCRAFT_MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    private static final Map<Path, String> WATHE_ROLE_INTERPRETATION_BOUNDARIES = Map.of(
            Path.of("src/main/java/org/caecorthus/strawcraft/StrawRoleMeaning.java"),
            "Central role meaning module; this is where gameplay meaning is named.\n" +
                    "集中解释模块；游戏语义只在这里命名。",
            Path.of("src/main/java/org/caecorthus/strawcraft/WatheRoleIds.java"),
            "Wathe role id constants; this is data, not behavior.\n" +
                    "Wathe 角色 id 常量；这里保存数据，不解释行为。",
            Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
            "Runtime bridge from official Wathe events into StrawCraft events.\n" +
                    "官方 Wathe 事件进入 StrawCraft 事件的运行时桥接边界。",
            Path.of("src/main/java/org/caecorthus/strawcraft/NoellesRuntimeRoleSelection.java"),
            "Runtime bridge that translates official Wathe role seats into StrawCraft role selection.\n" +
                    "把官方 Wathe 职业席位转换成 StrawCraft 职业选择的运行时桥接边界。"
    );

    private static final List<String> WATHE_ROLE_INTERPRETATION_TOKENS = List.of(
            ".canUseKiller()",
            ".isInnocent()",
            ".identifier()",
            "WatheRoleIds."
    );

    @Test
    void strawCraftRuntimeCodeUsesStrawRoleMeaningInsteadOfInterpretingWatheRolesDirectly() throws IOException {
        List<org.junit.jupiter.api.function.Executable> assertions = new ArrayList<>();
        for (Path boundary : WATHE_ROLE_INTERPRETATION_BOUNDARIES.keySet()) {
            assertions.add(() -> assertTrue(
                    Files.isRegularFile(boundary),
                    boundary + " should remain a documented role interpretation boundary"
            ));
        }
        for (Path path : strawCraftMainSources()) {
            if (WATHE_ROLE_INTERPRETATION_BOUNDARIES.containsKey(path)) {
                continue;
            }
            for (String token : WATHE_ROLE_INTERPRETATION_TOKENS) {
                assertions.add(() -> assertDoesNotContain(path, token));
            }
        }

        assertAll(assertions);
    }

    private static void assertDoesNotContain(Path path, String token) throws IOException {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        assertFalse(source.contains(token), path + " should ask StrawRoleMeaning instead of reading " + token);
    }

    private static List<Path> strawCraftMainSources() throws IOException {
        try (Stream<Path> sources = Files.walk(STRAWCRAFT_MAIN_ROOT)) {
            return sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }
}
