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

class BomberTimedBombRuntimeArchitectureTest {
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/TimedBombItem.java");
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/BomberTimedBombRuntime.java");
    private static final Path POLICY = Path.of("src/main/java/org/caecorthus/strawcraft/BomberTimedBombPolicy.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void itemUseIsServerAuthoritativeAndDelegatesChecksToPurePolicy() throws IOException {
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(item.contains("extends Item"));
        assertTrue(item.contains("useOnEntity"));
        assertTrue(item.contains("GameWorldComponent.KEY.get"));
        assertTrue(item.contains("game.isRunning()"));
        assertTrue(item.contains("GameFunctions.isPlayerAliveAndSurvival"));
        assertTrue(item.contains("NoellesRoleStateComponent.KEY.get(target)"));
        assertTrue(item.contains("BomberTimedBombPolicy.validateAttach"));
        assertTrue(item.contains("targetState.setTimedBomb"));
        assertTrue(item.contains("stack.decrement(1)"));
    }

    @Test
    void serverTickResolvesExpiryThroughOfficialWatheKillPipelineWithBombReason() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register"));
        assertTrue(runtime.contains("GameFunctions.killPlayer"));
        assertTrue(runtime.contains("StrawDeathReasons.BOMB"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(carrier)"));
        assertTrue(runtime.contains("state.clearTimedBomb()"));
        assertTrue(policy.contains("ExpiryResult.KILL_CARRIER"));
    }

    @Test
    void timedBombRuntimeAvoidsSparkImportsCustomEntitiesRenderersAndGenericAbilityFramework() throws IOException {
        for (Path path : mainJavaSources()) {
            String src = Files.readString(path, StandardCharsets.UTF_8);
            assertFalse(src.contains("import org.trainmurdermystery"), path + " must not import Spark Wathe runtime");
            assertFalse(src.contains("import org.noellesroles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(src.contains("XruiDD.NoellesRoles"), path + " must not depend on Spark NoellesRoles internals");
            assertFalse(src.contains("class TimedBombEntity"), path + " must not add a custom bomb entity");
            assertFalse(src.contains("TimedBombRenderer"), path + " must not add a custom bomb renderer");
            assertFalse(src.contains("GenericAbility"), path + " must not introduce a generic ability framework for this slice");
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
