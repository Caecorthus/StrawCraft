package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowingAxeRuntimeArchitectureTest {
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void itemReleaseSpawnsRegisteredProjectileAndConsumesOnlyAcceptedThrows() throws IOException {
        String item = Files.readString(MAIN_ROOT.resolve("ThrowingAxeItem.java"), StandardCharsets.UTF_8);

        assertTrue(item.contains("ThrowingAxePowerPolicy.evaluateChargeTicks"));
        assertTrue(item.contains("if (!decision.accepted())"));
        assertTrue(item.contains("new ThrowingAxeEntity(world, user, stack.copyWithCount(1))"));
        assertTrue(item.contains("world.spawnEntity(axe)"));
        assertTrue(item.contains("!player.getAbilities().creativeMode"));
        assertTrue(item.contains("stack.decrement(1)"));
    }

    @Test
    void projectileUsesThrowingAxeHitPolicyAndWatheDeathReason() throws IOException {
        String entity = Files.readString(MAIN_ROOT.resolve("ThrowingAxeEntity.java"), StandardCharsets.UTF_8);

        assertTrue(entity.contains("extends PersistentProjectileEntity"));
        assertTrue(entity.contains("PickupPermission.DISALLOWED"));
        assertTrue(entity.contains("ThrowingAxeHitPolicy"));
        assertTrue(entity.contains("GameFunctions.killPlayer(target, true, owner, StrawDeathReasons.THROWING_AXE)"));
        assertTrue(entity.contains("owner.canSee(target)"));
        assertTrue(entity.contains("target.getServerWorld() != owner.getServerWorld()"));
    }

    @Test
    void entityTypeIsRegisteredBeforeItemsAndClientHasRenderer() throws IOException {
        String entities = Files.readString(MAIN_ROOT.resolve("StrawCraftEntities.java"), StandardCharsets.UTF_8);
        String initializer = Files.readString(MAIN_ROOT.resolve("StrawCraft.java"), StandardCharsets.UTF_8);
        String client = Files.readString(MAIN_ROOT.resolve("client/StrawCraftClient.java"), StandardCharsets.UTF_8);

        assertTrue(entities.contains("THROWING_AXE"));
        assertTrue(entities.contains("Registry.register"));
        assertTrue(entities.contains("Registries.ENTITY_TYPE"));
        assertTrue(initializer.indexOf("StrawCraftEntities.register()") < initializer.indexOf("StrawCraftItems.register()"));
        assertTrue(client.contains("EntityRendererRegistry.register(StrawCraftEntities.THROWING_AXE"));
        assertTrue(client.contains("FlyingItemEntityRenderer::new"));
    }
}
