package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoodooBondRuntimeArchitectureTest {
    private static final Path RUNTIME = Path.of("src/main/java/org/caecorthus/strawcraft/VoodooBondRuntime.java");
    private static final Path PAYLOAD = Path.of("src/main/java/org/caecorthus/strawcraft/VoodooBondPayload.java");
    private static final Path CLIENT = Path.of("src/main/java/org/caecorthus/strawcraft/client/StrawCraftClient.java");
    private static final Path MOD_INITIALIZER = Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java");

    @Test
    void payloadCarriesExactlyOneTargetUuidAndNoClientCoordinatesOrRoleClaims() throws IOException {
        String payload = Files.readString(PAYLOAD, StandardCharsets.UTF_8);

        assertTrue(payload.contains("record VoodooBondPayload(UUID target)"));
        assertTrue(payload.contains("Uuids.PACKET_CODEC"));
        assertFalse(payload.contains("double"));
        assertFalse(payload.contains("Vec3d"));
        assertFalse(payload.contains("BlockPos"));
        assertFalse(payload.contains("Role"));
    }

    @Test
    void runtimeOwnsBondValidationAndStoresOnlyServerAcceptedTarget() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("PayloadTypeRegistry.playC2S().register(VoodooBondPayload.ID"));
        assertTrue(runtime.contains("ServerPlayNetworking.registerGlobalReceiver(VoodooBondPayload.ID"));
        assertTrue(runtime.contains("GameWorldComponent.KEY.get"));
        assertTrue(runtime.contains("game.isRunning()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(voodoo)"));
        assertTrue(runtime.contains("StrawRoleMeaning.receivesVoodooDeathBond"));
        assertTrue(runtime.contains("NoellesRoleStateComponent.KEY.get(voodoo)"));
        assertTrue(runtime.contains("roleState.isAbilityOnCooldown(VoodooBondPolicy.ABILITY_ID"));
        assertTrue(runtime.contains("voodoo.getServer().getPlayerManager().getPlayer(payload.target())"));
        assertTrue(runtime.contains("target.getServerWorld() == world"));
        assertTrue(runtime.contains("voodoo.squaredDistanceTo(target)"));
        assertTrue(runtime.contains("voodoo.canSee(target)"));
        assertTrue(runtime.contains("roleState.setVoodooBondedTarget(target.getUuid())"));
        assertTrue(runtime.contains("roleState.tryBeginAbilityCooldown"));
        assertTrue(runtime.indexOf("VoodooBondPolicy.validate") < runtime.indexOf("roleState.setVoodooBondedTarget"));
    }

    @Test
    void roleDeathCompletionKillsStillActiveBondedTargetThroughOfficialWathePipeline() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("StrawDeathEvents.ROLE_DEATH_COMPLETED.register(VoodooBondRuntime::handleRoleDeath)"));
        assertTrue(runtime.contains("context.victimRoleId().filter(VoodooBondPolicy.VOODOO_ROLE::equals).isEmpty()"));
        assertTrue(runtime.contains("voodooState.clearVoodooBondedTarget()"));
        assertTrue(runtime.contains("target.getServerWorld() == context.world()"));
        assertTrue(runtime.contains("GameFunctions.isPlayerAliveAndSurvival(target)"));
        assertTrue(runtime.contains("GameFunctions.killPlayer(target, true, null, StrawDeathReasons.VOODOO)"));
        assertTrue(runtime.indexOf("voodooState.clearVoodooBondedTarget()")
                < runtime.indexOf("GameFunctions.killPlayer(target, true, null, StrawDeathReasons.VOODOO)"));
    }

    @Test
    void voodooDeathChainsNoOpForAlreadyDeadOrSelfBondedTargets() throws IOException {
        String runtime = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(runtime.contains("if (target == null || target == voodoo)"));
        assertTrue(runtime.contains("!GameFunctions.isPlayerAliveAndSurvival(target)"));
    }

    @Test
    void commonAndClientRegistrationUseAimedPlayerUuidSelection() throws IOException {
        String initializer = Files.readString(MOD_INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);

        assertTrue(initializer.contains("VoodooBondRuntime.register()"));
        assertTrue(client.contains("\"key.strawcraft.voodoo_bond\""));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register(StrawCraftClient::tickVoodooBond)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new VoodooBondPayload(target.getUuid()))"));
    }

    @Test
    void voodooRuntimeAvoidsSparkNoellesParoxRuntimeDependencies() throws IOException {
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
