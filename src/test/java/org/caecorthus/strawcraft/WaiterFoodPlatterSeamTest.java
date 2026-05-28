package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WaiterFoodPlatterSeamTest {
    @Test
    void platterDoublePickupIsExplicitlyDeferredUntilTheOfficialBlockSeamIsSafe() throws IOException {
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/WaiterServiceItem.java"));
        String notes = Files.readString(Path.of("docs/waiter-service.md"));

        assertFalse(runtime.contains("FoodPlatterBlock"));
        assertTrue(notes.contains("FoodPlatterBlock double-pickup: deferred"));
    }
}
