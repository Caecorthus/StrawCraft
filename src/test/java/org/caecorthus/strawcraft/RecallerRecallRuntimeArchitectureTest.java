package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallerRecallRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/RecallerRecallRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/RecallerRecallPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void recallIntentIsServerAuthoritativeAndChargesBeforeTeleporting() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesRecallerRecall"));
        assertTrue(runtime.contains("PlayerShopComponent.KEY.get"));
        assertTrue(runtime.contains("shop.balance -= RecallerRecallPolicy.RECALL_PRICE"));
        assertTrue(runtime.contains("shop.sync()"));
        assertTrue(runtime.contains("recaller.stopRiding()"));
        assertTrue(runtime.contains("recaller.teleport"));
        assertTrue(runtime.contains("roleState.clearRecallerRecallPoint()"));
        assertTrue(runtime.indexOf("shop.balance -= RecallerRecallPolicy.RECALL_PRICE")
                < runtime.indexOf("recaller.teleport"));
    }

    @Test
    void payloadCarriesNoClientCoordinatesOrTargets() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record RecallerRecallPayload()"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("UUID"));
    }

    @Test
    void commonAndClientRegistrationUseEmptyIntentPayload() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("RecallerRecallRuntime.register()"));
        assertTrue(client.contains("KeyBindingHelper.registerKeyBinding"));
        assertTrue(client.contains("\"key.strawcraft.recaller_recall\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickRecallerRecall)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new RecallerRecallPayload())"));
    }

    @Test
    void recallerRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
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
