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

class ConductorMasterKeyRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/ConductorMasterKeyRuntime.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void conductorMasterKeyUsesOfficialWatheLockedDoorAllowanceEvent() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("dev.doctor4t.wathe.api.event.AllowPlayerOpenLockedDoor"));
        assertTrue(source.contains("AllowPlayerOpenLockedDoor.EVENT.register"));
    }

    @Test
    void conductorMasterKeyDoesNotUseSparkNoellesOrParoxDoorRuntimeApis() throws IOException {
        for (Path path : mainJavaSources()) {
            String source = Files.readString(path, StandardCharsets.UTF_8);

            assertFalse(source.contains("DoorInteraction"), path + " must not use Spark/Parox DoorInteraction");
            assertFalse(source.contains("XruiDD.NoellesRoles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(source.contains("wathe-Parox"), path + " must not target Parox Wathe");
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
