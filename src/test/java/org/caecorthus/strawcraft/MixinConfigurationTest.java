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

        assertTrue(serverPlayerMixin.contains("ModifyArgs"));
        assertTrue(serverPlayerMixin.contains("wathe$interceptVanillaDeath"));
        assertTrue(serverPlayerMixin.contains("WatheRoundParticipantLifecycle.afterVanillaDeath"));
        assertTrue(serverPlayerMixin.contains("consumeDeathAttribution"));
        assertTrue(serverPlayerMixin.contains("args.set(2"));
        assertTrue(serverPlayerMixin.contains("args.set(3"));
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
        assertTrue(roleAssignedLoadouts.contains("RoleAssigned.EVENT.register"));
        assertTrue(roleAssignedLoadouts.contains("RoundInventoryCleanup.removeDisabledWatheGuns"));
        assertTrue(roleAssignedLoadouts.contains("VigilanteLoadout.giveAssignedLoadout"));
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
}
