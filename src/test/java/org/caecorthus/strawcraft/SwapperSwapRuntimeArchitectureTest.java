package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwapperSwapRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/SwapperSwapRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/SwapperSwapPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void payloadCarriesExactlyTwoTargetUuidsAndNoClientCoordinates() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record SwapperSwapPayload(UUID targetA, UUID targetB)"));
        assertTrue(payload.contains("Uuids.PACKET_CODEC"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
    }

    @Test
    void runtimeOwnsRoundRoleAliveTargetCooldownDimensionAndSafetyChecks() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(SwapperSwapPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(SwapperSwapPayload.ID"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(swapper)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesSwapperSwap"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(swapper)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(SwapperSwapPolicy.ABILITY_ID"));
        assertTrue(runtime.contains("swapper.getServer().getPlayerManager().getPlayer(payload.targetA())"));
        assertTrue(runtime.contains("targetA.getServerWorld() == world"));
        assertTrue(runtime.contains("world.getWorldBorder().contains"));
        assertTrue(runtime.contains("world.getBlockCollisions"));
        assertTrue(runtime.contains("player.stopRiding()"));
        assertTrue(runtime.contains("targetA.teleport(world"));
        assertTrue(runtime.contains("targetB.teleport(world"));
        assertTrue(runtime.contains("roleState.tryBeginAbilityCooldown"));
        assertTrue(runtime.indexOf("SwapperSwapPolicy.validate") < runtime.indexOf("targetA.teleport(world"));
        assertTrue(runtime.indexOf("targetB.teleport(world") < runtime.indexOf("roleState.tryBeginAbilityCooldown"));
    }

    @Test
    void commonAndClientRegistrationUseTwoStepHoveredPlayerSelection() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("SwapperSwapRuntime.register()"));
        assertTrue(client.contains("\"key.strawcraft.swapper_swap\""));
        assertTrue(client.contains("pendingSwapperTarget"));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickSwapperSwap)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new SwapperSwapPayload(pendingSwapperTarget, targetUuid))"));
    }

    @Test
    void swapperRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";
        for (String source : java.util.List.of(runtime, payload, client)) {
            assertFalse(source.contains(trainmurdermysteryImport));
            assertFalse(source.contains(noellesRolesImport));
            assertFalse(source.contains(xruiNoellesRuntime));
            assertFalse(source.contains(paroxWathe));
        }
    }
}
