package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorphlingDisguiseRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/MorphlingDisguiseRuntime.java");
    private static final Path MORPH_PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/MorphlingDisguisePayload.java");
    private static final Path CORPSE_PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/MorphlingCorpseTogglePayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void payloadsCarryOnlyServerValidatedIntent() throws IOException {
        String morphPayload = Files.readString(MORPH_PAYLOAD, StandardCharsets.UTF_8);
        String corpsePayload = Files.readString(CORPSE_PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(morphPayload.contains("record MorphlingDisguisePayload(UUID target)"));
        assertTrue(morphPayload.contains("new Id<>(StrawCraft.id(\"morph\"))"));
        assertTrue(morphPayload.contains("Uuids.PACKET_CODEC"));
        assertTrue(corpsePayload.contains("record MorphlingCorpseTogglePayload()"));
        assertTrue(corpsePayload.contains("new Id<>(StrawCraft.id(\"morph_corpse_toggle\"))"));
        assertTrue(corpsePayload.contains("PacketCodec.unit(new MorphlingCorpseTogglePayload())"));
        for (String source : java.util.List.of(morphPayload, corpsePayload)) {
            assertFalse(source.contains("double"));
            assertFalse(source.contains("Vec3d"));
            assertFalse(source.contains("BlockPos"));
            assertFalse(source.contains("Role"));
        }
    }

    @Test
    void runtimeOwnsMorphlingRoleTargetCooldownRecoveryAndTickValidation() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(MorphlingDisguisePayload.ID"));
        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(MorphlingCorpseTogglePayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(MorphlingDisguisePayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(MorphlingCorpseTogglePayload.ID"));
        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register(MorphlingDisguiseRuntime::tickServer)"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesMorphlingDisguise"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(morphling)"));
        assertTrue(runtime.contains("morphling.getServer().getPlayerManager().getPlayer(payload.target())"));
        assertTrue(runtime.contains("target.getServerWorld() == world"));
        assertTrue(runtime.contains("game.getRole(target) != null"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(target)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(MorphlingDisguisePolicy.ABILITY_ID"));
        assertTrue(runtime.contains("roleState.morphlingDisguiseState().morphTicks() == 0"));
        assertTrue(runtime.contains("MorphlingDisguisePolicy.startMorph"));
        assertTrue(runtime.contains("MorphlingDisguisePolicy.stopMorph"));
        assertTrue(runtime.contains("MorphlingDisguisePolicy.toggleCorpseMode"));
        assertTrue(runtime.contains("MorphlingDisguisePolicy.tick"));
        assertTrue(runtime.contains("roleState.setAbilityCooldown(MorphlingDisguisePolicy.ABILITY_ID"));
        assertTrue(runtime.indexOf("MorphlingDisguisePolicy.validateStart") < runtime.indexOf("MorphlingDisguisePolicy.startMorph"));
    }

    @Test
    void commonRegistrationIsServerOnlyAndMorphlingStaysOutOfRuntimeSelection() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        Set<net.minecraft.util.Identifier> runtimeIds = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(org.caecorthus.strawcraft.role.StrawRoleDefinition::id)
                .collect(Collectors.toSet());

        assertTrue(initializer.contains("MorphlingDisguiseRuntime.register()"));
        assertFalse(client.contains("MorphlingDisguisePayload"));
        assertFalse(client.contains("MorphlingCorpseTogglePayload"));
        assertEquals(NoellesRoleCatalog.Readiness.DESIGN_REQUIRED,
                NoellesRoleCatalog.find(StrawCraft.id("morphling")).orElseThrow().readiness());
        assertFalse(runtimeIds.contains(StrawCraft.id("morphling")));
    }

    @Test
    void morphlingRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String morphPayload = Files.readString(MORPH_PAYLOAD, StandardCharsets.UTF_8);
        String corpsePayload = Files.readString(CORPSE_PAYLOAD, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, morphPayload, corpsePayload)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
