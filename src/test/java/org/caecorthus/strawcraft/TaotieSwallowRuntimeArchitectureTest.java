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

class TaotieSwallowRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/TaotieSwallowRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/TaotieSwallowPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");
    private static final Path NEUTRAL_WIN_POLICY = Path.of("src/main/java/org/caecorthus/strawcraft/NoellesNeutralWinPolicy.java");

    @Test
    void payloadCarriesExactlyOneTargetUuidAndNoClientCoordinatesOrRoleClaims() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record TaotieSwallowPayload(UUID target)"));
        assertTrue(payload.contains("new Id<>(StrawCraft.id(\"taotie_swallow\"))"));
        assertTrue(payload.contains("Uuids.PACKET_CODEC"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
        assertFalse(payload.contains("Role"));
    }

    @Test
    void runtimeOwnsValidationSwallowedMarkingSpectatorCameraCleanupAndWinClaim() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String neutralWinPolicy = Files.readString(NEUTRAL_WIN_POLICY, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(TaotieSwallowPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(TaotieSwallowPayload.ID"));
        assertTrue(runtime.contains("ServerTickEvents.END_SERVER_TICK.register(TaotieSwallowRuntime::tickServer)"));
        assertTrue(runtime.contains("ServerPlayConnectionEvents.DISCONNECT.register"));
        assertTrue(runtime.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register"));
        assertTrue(runtime.contains("StrawRoleEvents.ROLE_ASSIGNED.register"));
        assertTrue(runtime.contains("StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register"));
        assertTrue(runtime.contains("GameEvents.ON_FINISH_FINALIZE.register"));
        assertTrue(runtime.contains("releaseSwallowedTarget(server, player.getUuid(), player)"));
        assertTrue(runtime.contains("releaseAllSwallowedBy(server, player.getUuid(), player)"));
        assertTrue(runtime.contains("TaotieSwallowPolicy.validateSwallow"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesTaotieSwallow"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(taotie)"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(target)"));
        assertTrue(runtime.contains("target.isAlive()"));
        assertTrue(runtime.contains("taotie.squaredDistanceTo(target) <= TaotieSwallowPolicy.SWALLOW_DISTANCE_SQUARED"));
        assertTrue(runtime.contains("taotie.canSee(target)"));
        assertTrue(runtime.contains("taotieState.trackTaotieSwallowedPlayer(targetUuid)"));
        assertTrue(runtime.contains("targetState.setTaotieSwallowedBy(taotieUuid)"));
        assertTrue(runtime.contains("GameMode.SPECTATOR"));
        assertTrue(runtime.contains("target.setCameraEntity(taotie)"));
        assertTrue(runtime.contains("target.setCameraEntity(target)"));
        assertTrue(runtime.contains("GameMode.SURVIVAL"));
        assertTrue(runtime.contains("releaseAllSwallowedBy(context.world().getServer(), victimUuid)"));
        assertTrue(runtime.contains("releaseSwallowedTarget(context.world().getServer(), victimUuid)"));
        assertTrue(runtime.contains("replaceDefaultWin(StrawWinEvents.DefaultWin.LOOSE_END)"));
        assertTrue(runtime.contains("TaotieSwallowPolicy.hasSwallowedEveryone"));
        assertFalse(runtime.contains("contribution.suppressDefaultWin()"));
        assertTrue(neutralWinPolicy.contains("TaotieSwallowPolicy.TAOTIE_ROLE"));
        assertTrue(runtime.indexOf("TaotieSwallowPolicy.validateSwallow") < runtime.indexOf("swallow(taotie, target"));
        assertFalse(runtime.contains("GameFunctions.killPlayer(target"));
        assertFalse(runtime.contains("spawnBody"));
    }

    @Test
    void commonAndClientRegistrationUseAimedPlayerUuidSelection() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        Set<net.minecraft.util.Identifier> runtimeIds = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(org.caecorthus.strawcraft.role.StrawRoleDefinition::id)
                .collect(Collectors.toSet());

        assertTrue(initializer.contains("TaotieSwallowRuntime.register()"));
        assertTrue(client.contains("TaotieSwallowPayload"));
        assertTrue(client.contains("\"key.strawcraft.taotie_swallow\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickTaotieSwallow)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new TaotieSwallowPayload(target.getUuid()))"));
        assertEquals(NoellesRoleCatalog.Readiness.RUNTIME_READY,
                NoellesRoleCatalog.find(TaotieSwallowPolicy.TAOTIE_ROLE).orElseThrow().readiness());
        assertTrue(runtimeIds.contains(TaotieSwallowPolicy.TAOTIE_ROLE));
    }

    @Test
    void taotieRuntimeKeepsVoicePluginOutOfMvpAndAvoidsSparkNoellesParoxDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, payload, client)) {
            assertFalse(source.contains("VoiceChat"));
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
            assertFalse(source.contains("CheckWinCondition"));
        }
    }
}
