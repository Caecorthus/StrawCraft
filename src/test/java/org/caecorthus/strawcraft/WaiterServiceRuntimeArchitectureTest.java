package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WaiterServiceRuntimeArchitectureTest {
    @Test
    void waiterRuntimeBridgesHeldFoodAndCocktailsIntoTheServicePath() throws IOException {
        String initializer = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"));
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/WaiterServiceRuntime.java"));
        String item = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/WaiterServiceItem.java"));

        assertTrue(initializer.contains("WaiterServiceRuntime.register();"));
        assertTrue(runtime.contains("UseEntityCallback.EVENT.register"));
        assertTrue(runtime.contains("player.getStackInHand(hand)"));
        assertTrue(runtime.contains("WaiterServiceItem.serviceKindFor(stack)"));
        assertTrue(runtime.contains("WaiterServiceItem.tryServe(stack, player, entity, hand)"));
        assertTrue(item.contains("static ActionResult tryServe"));
        assertTrue(item.indexOf("useOnEntity") < item.indexOf("return tryServe(stack, user, entity, hand);"));
    }

    @Test
    void waiterServiceValidatesRoleAndOpenWatheTasksBeforeMutatingMood() throws IOException {
        String source = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/WaiterServiceItem.java"));

        assertTrue(source.contains("GameWorldComponent.KEY.get(waiter.getWorld()).getRole(waiter)"));
        assertTrue(source.contains("GameFunctions.isPlayerAliveAndSurvival(waiter)"));
        assertTrue(source.contains("GameFunctions.isPlayerAliveAndSurvival(target)"));
        assertTrue(source.contains("PlayerMoodComponent.Task.EAT"));
        assertTrue(source.contains("PlayerMoodComponent.Task.DRINK"));
        assertTrue(source.indexOf("WaiterFeedingPolicy.chooseService") < source.indexOf("satisfyTask(targetMood"));
    }

    @Test
    void officialFoodAndCocktailServingUsesWatheConsumptionSoPoisonComponentsStayAttached() throws IOException {
        String source = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/WaiterServiceItem.java"));

        assertTrue(source.contains("stack.finishUsing(target.getWorld(), target)"));
        assertTrue(source.contains("stack.getItem() instanceof CocktailItem"));
        assertTrue(source.contains("DataComponentTypes.FOOD"));
        assertFalse(source.contains("PoisonUtils"));
    }
}
