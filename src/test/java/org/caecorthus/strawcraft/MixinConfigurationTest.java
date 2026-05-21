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
    void vanillaDeathStillEntersWatheDeathPipelineWithTrackedReason() throws IOException {
        String serverPlayerMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/ServerPlayerEntityMixin.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(serverPlayerMixin.contains("ModifyArgs"));
        assertFalse(serverPlayerMixin.contains("wathe$interceptVanillaDeath"));
        assertTrue(serverPlayerMixin.contains("WatheRoundParticipantLifecycle.afterVanillaDeath"));
        assertTrue(serverPlayerMixin.contains("onDeath"));
    }

    @Test
    void grenadeDamageOverrideIsRegisteredServerSide() throws IOException {
        String config = readMixinConfig();
        String grenadeMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/GrenadeEntityMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(config.contains("\"GrenadeEntityMixin\""));
        assertTrue(grenadeMixin.contains("GameFunctions;killPlayer"));
        assertTrue(grenadeMixin.contains("Explosion.getExposure"));
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
        assertFalse(shopScreen.contains("ShopUtils"));
        assertFalse(shopScreen.contains("PlayerShopComponent"));
        assertFalse(shopScreen.contains("StoreBuyPayload"));
        assertFalse(slotRenderer.contains("LimitedInventoryScreen"));
        assertFalse(slotRenderer.contains("StoreItemWidget"));
        assertTrue(slotRenderer.contains("entry.type().getTexture()"));

        String adapter = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/WatheShopClientAdapter.java"),
                StandardCharsets.UTF_8
        );
        assertTrue(adapter.contains("canUseKillerFeatures"));
    }

    @Test
    void roleAssignedAdapterOwnsRoleLoadoutEventRegistration() throws IOException {
        String strawCraft = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"), StandardCharsets.UTF_8);
        String vigilanteLoadout = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/VigilanteLoadout.java"),
                StandardCharsets.UTF_8
        );
        String roleAssignedLoadouts = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(strawCraft.contains("RoleAssignedLoadouts.register()"));
        assertFalse(vigilanteLoadout.contains("RoleAssigned.EVENT"));
        assertTrue(roleAssignedLoadouts.contains("StrawRoleEvents.ROLE_ASSIGNED.register"));
        assertTrue(roleAssignedLoadouts.contains("RoundInventoryCleanup.removeDisabledWatheGuns"));
        assertTrue(roleAssignedLoadouts.contains("VigilanteLoadout.giveAssignedLoadout"));
    }

    @Test
    void strawCraftDoesNotDependOnSparkOnlyWatheEventApis() throws IOException {
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.BuildShopEntries");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.KillPlayer");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.RoleAssigned");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.util.ShopUtils");
        assertSourceTreeDoesNotContain("new ShopEntry.Builder");
        assertSourceTreeDoesNotContain("WatheRoles.NO_ROLE");
        assertSourceTreeDoesNotContain("WatheRoles.VETERAN");
        assertSourceTreeDoesNotContain("game.markPlayerDead");
        assertSourceTreeDoesNotContain("game.hasAnyRole");
        assertSourceTreeDoesNotContain("game.isPlayerDead");
    }

    @Test
    void strawCraftOwnsMapVotingInsteadOfReferencingParoxVotingClasses() throws IOException {
        String modJson = Files.readString(Path.of("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);
        String mixinConfig = readMixinConfig();
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/map/StrawMapVoting.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(modJson.contains("org.caecorthus.strawcraft.client.StrawCraftClient"));
        assertTrue(modJson.contains("org.caecorthus.strawcraft.map.StrawCraftComponents"));
        assertTrue(modJson.contains("strawcraft:map_voting"));
        assertTrue(mixinConfig.contains("\"GameFunctionsMixin\""));
        assertFalse(source.contains("dev.doctor4t.wathe.cca.MapVotingComponent"));
        assertFalse(source.contains("dev.doctor4t.wathe.config.datapack.MapRegistry"));
        assertFalse(source.contains("dev.doctor4t.wathe.util.MapVotePayload"));
        assertFalse(source.contains("finalizeVoting"));
    }

    private static String readMixinConfig() throws IOException {
        return Files.readString(Path.of("src/main/resources/strawcraft.mixins.json"), StandardCharsets.UTF_8);
    }

    private static void assertSourceTreeDoesNotContain(String forbidden) throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                assertFalse(source.contains(forbidden), path + " should not contain " + forbidden);
            }
        }
    }
}
