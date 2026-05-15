package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinConfigurationTest {
    @Test
    void vanillaInventoryRestorerIsAClientOnlyMixin() throws IOException {
        String config = readMixinConfig();

        assertTrue(config.contains("\"client\""));
        assertTrue(config.contains("\"client.VanillaInventoryScreenMixin\""));
    }

    @Test
    void inventoryShopTabIsAClientOnlyMixin() throws IOException {
        String config = readMixinConfig();

        assertTrue(config.contains("\"client\""));
        assertTrue(config.contains("\"client.InventoryShopTabMixin\""));
    }

    @Test
    void vanillaDeathStillEntersWatheDeathPipeline() throws IOException {
        String serverPlayerMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/ServerPlayerEntityMixin.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(serverPlayerMixin.contains("wathe$interceptVanillaDeath"));
    }

    @Test
    void customShopScreenDoesNotInstantiateWatheLimitedInventoryUi() throws IOException {
        String shopScreen = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftShopScreen.java"),
                StandardCharsets.UTF_8
        );
        String slotRenderer = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/WatheShopSlotRenderer.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(shopScreen.contains("LimitedInventoryScreen"));
        assertFalse(shopScreen.contains("StoreItemWidget"));
        assertFalse(slotRenderer.contains("LimitedInventoryScreen"));
        assertFalse(slotRenderer.contains("StoreItemWidget"));
        assertTrue(slotRenderer.contains("entry.type().getTexture()"));
    }

    private static String readMixinConfig() throws IOException {
        return Files.readString(Path.of("src/main/resources/strawcraft.mixins.json"), StandardCharsets.UTF_8);
    }
}
