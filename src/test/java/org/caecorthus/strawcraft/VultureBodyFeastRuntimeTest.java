package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VultureBodyFeastRuntimeTest {
    @Test
    void validationRejectsNonVultureCooldownAndMissingBody() {
        assertEquals(
                VultureBodyFeastRuntime.ValidationResult.NOT_VULTURE,
                VultureBodyFeastRuntime.validateInteraction(input(true, false, true, true, true, 4.0D))
        );
        assertEquals(
                VultureBodyFeastRuntime.ValidationResult.COOLDOWN,
                VultureBodyFeastRuntime.validateInteraction(input(true, true, true, false, true, 4.0D))
        );
        assertEquals(
                VultureBodyFeastRuntime.ValidationResult.NO_BODY,
                VultureBodyFeastRuntime.validateInteraction(input(true, true, true, true, false, 4.0D))
        );
    }

    @Test
    void validationAllowsAliveVultureNearBody() {
        assertEquals(
                VultureBodyFeastRuntime.ValidationResult.ALLOWED,
                VultureBodyFeastRuntime.validateInteraction(input(true, true, true, true, true, 24.9D))
        );
    }

    @Test
    void runtimeEffectsOnlyFollowAllowedAcceptedFeasts() {
        VultureBodyFeastPolicy.FeastResult accepted =
                new VultureBodyFeastPolicy.FeastResult(true, false, 1, 2, false);
        VultureBodyFeastPolicy.FeastResult duplicate =
                new VultureBodyFeastPolicy.FeastResult(false, true, 1, 2, false);

        VultureBodyFeastRuntime.FeastRuntimeEffects allowedAccepted =
                VultureBodyFeastRuntime.planRuntimeEffects(
                        VultureBodyFeastRuntime.ValidationResult.ALLOWED,
                        accepted
                );
        assertTrue(allowedAccepted.beginCooldown());
        assertTrue(allowedAccepted.consumeBody());
        assertTrue(allowedAccepted.sendProgressMessage());

        VultureBodyFeastRuntime.FeastRuntimeEffects duplicateEffects =
                VultureBodyFeastRuntime.planRuntimeEffects(
                        VultureBodyFeastRuntime.ValidationResult.ALLOWED,
                        duplicate
                );
        assertTrue(duplicateEffects.skipAll());

        VultureBodyFeastRuntime.FeastRuntimeEffects rejectedEffects =
                VultureBodyFeastRuntime.planRuntimeEffects(
                        VultureBodyFeastRuntime.ValidationResult.COOLDOWN,
                        accepted
                );
        assertTrue(rejectedEffects.skipAll());
    }

    @Test
    void vultureFeastHasProductionPayloadServerBodySearchAndClientKeybind() throws IOException {
        String strawCraft = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"), StandardCharsets.UTF_8);
        String runtime = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/VultureBodyFeastRuntime.java"), StandardCharsets.UTF_8);
        String payload = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/VultureFeastPayload.java"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java"), StandardCharsets.UTF_8);
        String loadouts = Files.readString(Path.of("src/main/java/org/caecorthus/strawcraft/RoleAssignedLoadouts.java"), StandardCharsets.UTF_8);

        assertTrue(strawCraft.contains("VultureBodyFeastRuntime.register()"));
        assertTrue(loadouts.contains("VultureBodyFeastPolicy.resetRoundState"));
        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(VultureFeastPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(VultureFeastPayload.ID"));
        assertTrue(runtime.contains("PlayerBodyEntity"));
        assertTrue(runtime.contains("TypeFilter.equals(PlayerBodyEntity.class)"));
        assertTrue(runtime.contains("getBoundingBox().expand(VultureBodyFeastPolicy.FEAST_RANGE)"));
        assertTrue(runtime.contains("body.discard()"));
        assertTrue(payload.contains("vulture_feast"));
        assertTrue(client.contains("vultureFeastKey"));
        assertTrue(client.contains("ClientPlayNetworking.send(new VultureFeastPayload())"));
    }

    private static VultureBodyFeastRuntime.InteractionInput input(
            boolean gameRunning,
            boolean vultureRole,
            boolean playerAlive,
            boolean cooldownReady,
            boolean bodyFound,
            double nearestBodyDistanceSquared
    ) {
        return new VultureBodyFeastRuntime.InteractionInput(
                gameRunning,
                vultureRole,
                playerAlive,
                cooldownReady,
                bodyFound,
                nearestBodyDistanceSquared
        );
    }
}
