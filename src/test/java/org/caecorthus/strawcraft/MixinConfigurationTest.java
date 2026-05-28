package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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
        String lifecycle = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheRoundParticipantLifecycle.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(serverPlayerMixin.contains("ModifyArgs"));
        assertFalse(serverPlayerMixin.contains("wathe$interceptVanillaDeath"));
        assertTrue(serverPlayerMixin.contains("WatheRoundParticipantLifecycle.afterVanillaDeath"));
        assertTrue(serverPlayerMixin.contains("onDeath"));
        assertTrue(lifecycle.contains("KillRewardPayout.payoutVanillaDeath(player, game, attribution)"));
        assertTrue(lifecycle.contains("GameFunctions.killPlayer("));
        assertTrue(lifecycle.contains("forward.deathReason()"));
        assertFalse(lifecycle.contains(".map(killerUuid -> player.getServer().getPlayerManager().getPlayer(killerUuid))"));
    }

    @Test
    void taczGoodOnGoodShotFeedsUnattributedShotInnocentRewardPool() throws IOException {
        String playerMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerEntityMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(playerMixin.contains("rememberShotInnocentDeath"));
        assertTrue(playerMixin.contains("game.isInnocent(victim) && game.isInnocent(attacker)"));
        assertFalse(playerMixin.contains("GameConstants.DeathReasons.SHOT_INNOCENT"));
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
    void officialWatheMixinTargetsDoNotUseParoxOnlySoftTargets() throws IOException {
        assertProductionSurfaceDoesNotContain("wathe$cancelApplyDamage");
        assertProductionSurfaceDoesNotContain("wathe$restrictJump");
    }

    @Test
    void killPlayerRedirectDoesNotUseParoxServerPlayerDescriptor() throws IOException {
        assertProductionSurfaceDoesNotContain(
                "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer("
                        + "Lnet/minecraft/server/network/ServerPlayerEntity;Z"
                        + "Lnet/minecraft/server/network/ServerPlayerEntity;"
                        + "Lnet/minecraft/util/Identifier;)V"
        );
    }

    @Test
    void killPlayerRedirectUsesOfficialWathePlayerDescriptor() throws IOException {
        String grenadeMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/GrenadeEntityMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(grenadeMixin.contains(
                "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer("
                        + "Lnet/minecraft/entity/player/PlayerEntity;Z"
                        + "Lnet/minecraft/entity/player/PlayerEntity;"
                        + "Lnet/minecraft/util/Identifier;)V"
        ), "GrenadeEntityMixin should target official Wathe killPlayer(PlayerEntity, boolean, PlayerEntity, Identifier)");
    }

    @Test
    void strawCraftOnlyTargetsOfficialWatheBodiesForBodyOwnedRoleSlices() throws IOException {
        Path vultureRuntime = Path.of("src/main/java/org/caecorthus/strawcraft/VultureBodyFeastRuntime.java");
        Path coronerRuntime = Path.of("src/main/java/org/caecorthus/strawcraft/CoronerInspectionRuntime.java");
        Path scavengerRuntime = Path.of("src/main/java/org/caecorthus/strawcraft/ScavengerHiddenBodies.java");
        Path scavengerClientVisibility =
                Path.of("src/main/java/org/caecorthus/strawcraft/ScavengerHiddenBodyClientVisibility.java");
        Path gameFunctionsMixin = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/GameFunctionsMixin.java");
        Path playerBodyMixin = Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerBodyEntityMixin.java");
        Path playerBodyRendererMixin =
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/client/PlayerBodyEntityRendererMixin.java");
        assertSourceTreeDoesNotContainExcept("PlayerBodyEntity", Set.of(
                vultureRuntime,
                coronerRuntime,
                scavengerRuntime,
                scavengerClientVisibility,
                gameFunctionsMixin,
                playerBodyMixin,
                playerBodyRendererMixin
        ));
        assertProductionSurfaceDoesNotContain("WatheEntities.PLAYER_BODY");
        String runtime = Files.readString(vultureRuntime, StandardCharsets.UTF_8);
        assertTrue(runtime.contains("dev.doctor4t.wathe.entity.PlayerBodyEntity"));
        assertTrue(runtime.contains("TypeFilter.equals(PlayerBodyEntity.class)"));
        String coroner = Files.readString(coronerRuntime, StandardCharsets.UTF_8);
        assertTrue(coroner.contains("dev.doctor4t.wathe.entity.PlayerBodyEntity"));
        assertTrue(coroner.contains("TypeFilter.equals(PlayerBodyEntity.class)"));
    }

    @Test
    void corpseMetadataClearsOnWatheRoundBoundaries() throws IOException {
        String officialBridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(officialBridge.contains("GameEvents.ON_FINISH_INITIALIZE"));
        assertTrue(officialBridge.contains("GameEvents.ON_FINISH_FINALIZE"));
        assertTrue(officialBridge.contains("StrawCorpseMetadata.clearAll();"));
    }

    @Test
    void scavengerHiddenBodyStateClearsOnWatheRoundBoundaries() throws IOException {
        String scavengerHiddenBodies = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/ScavengerHiddenBodies.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(scavengerHiddenBodies.contains("GameEvents.ON_FINISH_INITIALIZE.register((world, game) -> clearRoundWorld(world))"));
        assertTrue(scavengerHiddenBodies.contains("GameEvents.ON_FINISH_FINALIZE.register((world, game) -> clearRoundWorld(world))"));
        assertTrue(scavengerHiddenBodies.contains("body).strawcraft$setHiddenByScavenger(false)"));
        assertTrue(scavengerHiddenBodies.contains("STATE.clearWorld(serverWorld.getRegistryKey().getValue())"));
    }

    @Test
    void scavengerHiddenBodyTargetingIsOwnedByPlayerBodyMixin() throws IOException {
        String config = readMixinConfig();
        String playerBodyMixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerBodyEntityMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(config.contains("\"PlayerBodyEntityMixin\""));
        assertTrue(playerBodyMixin.contains("canBeHitByProjectile()"));
        assertTrue(playerBodyMixin.contains("canHit()"));
        assertTrue(playerBodyMixin.contains("isAttackable()"));
        assertTrue(playerBodyMixin.contains("ScavengerHiddenBodyTargeting.allowsTargeting"));
        assertTrue(playerBodyMixin.contains("strawcraft$isHiddenByScavenger()"));
        assertTrue(playerBodyMixin.contains("super.canBeHitByProjectile()"));
        assertTrue(playerBodyMixin.contains("super.canHit()"));
        assertTrue(playerBodyMixin.contains("super.isAttackable()"));
        assertFalse(playerBodyMixin.contains("return false;"));
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
        assertFalse(slotRenderer.contains("dev.doctor4t.wathe.util.ShopEntry"));
        assertFalse(slotRenderer.contains("StrawShopEntry"));
        assertFalse(slotRenderer.contains("entry.type()"));
        assertFalse(slotRenderer.contains("displayStackFor"));
        assertTrue(slotRenderer.contains("state.type().getTexture()"));
        assertTrue(slotRenderer.contains("state.displayStack()"));

        String adapter = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/WatheShopClientAdapter.java"),
                StandardCharsets.UTF_8
        );
        assertTrue(adapter.contains("PlayerShopCatalog.presentationFor"));
    }

    @Test
    void customShopScreenBuysFromEntryViewStateIndex() throws IOException {
        String shopScreen = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftShopScreen.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(shopScreen.contains("this.shopAdapter.buy(this.visibleIndex)"));
        assertFalse(shopScreen.contains("state.purchaseIndex()"));
        assertFalse(shopScreen.contains("this.shopAdapter.buy(this.index)"));
    }

    @Test
    void clientShopAdapterUsesAlreadyMaterializedOfficialEntries() throws IOException {
        String adapter = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/client/WatheShopClientAdapter.java"),
                StandardCharsets.UTF_8
        );
        String officialBridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(adapter.contains("StrawShopEvents.modifyEntries"));
        assertTrue(adapter.contains("GameConstants.SHOP_ENTRIES"));
        assertTrue(officialBridge.contains("StrawShopEvents.buildEntries(originalEntries)"));
    }

    @Test
    void playerShopComponentPurchaseFlowIsOwnedByStrawCraftMixin() throws IOException {
        String config = readMixinConfig();
        String mixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerShopComponentMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(config.contains("\"PlayerShopComponentMixin\""));
        assertTrue(mixin.contains("StrawShopPurchaseFlow.tryBuy"));
        assertTrue(mixin.contains("callback.cancel()"));
        assertFalse(mixin.contains("import dev.doctor4t.wathe.util.StoreBuyPayload"));
        assertFalse(mixin.contains("new StoreBuyPayload"));
    }

    @Test
    void playerMoodTaskCompletionRewardRunsAfterOfficialNotificationWithoutCancellingIt() throws IOException {
        String config = readMixinConfig();
        String mixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerMoodComponentMixin.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(config.contains("\"PlayerMoodComponentMixin\""));
        assertTrue(mixin.contains("method = \"serverTick\""));
        assertTrue(mixin.contains("ServerPlayNetworking;send(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/packet/CustomPayload;)V"));
        assertTrue(mixin.contains("shift = At.Shift.AFTER"));
        assertTrue(mixin.contains("require = 0"));
        assertTrue(mixin.contains("TaskCompletionRewardPayout.payoutTaskCompletion(serverPlayer)"));
        assertFalse(mixin.contains("cancellable = true"));
        assertFalse(mixin.contains("callback.cancel()"));
    }

    @Test
    void shopEntryBuilderCannotForkOrderByPlayerSpecificState() throws IOException {
        String events = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/api/StrawShopEvents.java"),
                StandardCharsets.UTF_8
        );
        String flow = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/StrawShopPurchaseFlow.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(events.contains("void buildEntries(ShopContext context)"));
        assertTrue(events.contains("MODIFY_ENTRIES.invoker().modifyEntries(null, context)"));
        assertTrue(events.contains("return buildEntries(baseEntries);"));
        assertTrue(flow.contains("List.copyOf(baseEntries)"));
        assertFalse(flow.contains("StrawShopEvents.buildEntries(baseEntries)"));
        assertFalse(flow.contains("StrawShopEvents.buildEntries(player, baseEntries)"));
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
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.CheckWinCondition");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.KillPlayer");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.RoleAssigned");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.RoleAppearanceCondition");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.api.event.RoleSelectionContext");
        assertSourceTreeDoesNotContain("dev.doctor4t.noellesroles");
        assertSourceTreeDoesNotContain("noellesroles.mixin");
        assertSourceTreeDoesNotContain("dev.doctor4t.wathe.util.ShopUtils");
        assertSourceTreeDoesNotContain("neutral_master_key");
        assertSourceTreeDoesNotContain("new ShopEntry.Builder");
        assertSourceTreeDoesNotContain("WatheRoles.NO_ROLE");
        assertSourceTreeDoesNotContain("WatheRoles.VETERAN");
        assertSourceTreeDoesNotContain("game.markPlayerDead");
        assertSourceTreeDoesNotContain("game.hasAnyRole");
        assertSourceTreeDoesNotContain("game.isPlayerDead");
        assertSourceTreeDoesNotContain("WinStatus.NEUTRAL");
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

    @Test
    void noellesRoleStateComponentIsRegisteredAsStrawCraftOwnedPlayerCca() throws IOException {
        String modJson = Files.readString(Path.of("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);
        String components = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/map/StrawCraftComponents.java"),
                StandardCharsets.UTF_8
        );
        String bridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(modJson.contains("strawcraft:noelles_role_state"));
        assertTrue(components.contains("NoellesRoleStateComponent.KEY"));
        assertTrue(components.contains("RespawnCopyStrategy.NEVER_COPY"));
        assertTrue(bridge.contains("resetNoellesRoleState"));
    }

    @Test
    void noellesRuntimeRoleSelectionUsesPostAssignmentBridgeInsteadOfSparkSelectorMixins() throws IOException {
        String mixinConfig = readMixinConfig();
        String bridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        assertFalse(mixinConfig.contains("ScoreboardRoleSelectorComponent"));
        assertTrue(bridge.contains("NoellesRuntimeRoleSelection.replaceEligibleOfficialAssignments"));
    }

    private static String readMixinConfig() throws IOException {
        return Files.readString(Path.of("src/main/resources/strawcraft.mixins.json"), StandardCharsets.UTF_8);
    }

    private static void assertSourceTreeDoesNotContain(String forbidden) throws IOException {
        assertSourceTreeDoesNotContainExcept(forbidden, Set.of());
    }

    private static void assertSourceTreeDoesNotContainExcept(String forbidden, Set<Path> allowedPaths) throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                if (allowedPaths.contains(path) && source.contains(forbidden)) {
                    continue;
                }
                assertFalse(source.contains(forbidden), path + " should not contain " + forbidden);
            }
        }
    }

    private static void assertProductionSurfaceDoesNotContain(String forbidden) throws IOException {
        assertSourceTreeDoesNotContain(forbidden);
        assertFalse(readMixinConfig().contains(forbidden), "mixin config should not contain " + forbidden);
    }
}
