package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoisemakerRuntimeArchitectureTest {
    private static final Path ITEM = Path.of("src/main/java/org/caecorthus/strawcraft/NoisemakerItem.java");
    private static final Path POLICY = Path.of("src/main/java/org/caecorthus/strawcraft/NoisemakerEffectPolicy.java");
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");

    @Test
    void noisemakerItemChecksOfficialGameRoleAndAliveRunningRoundSeams() throws IOException {
        String src = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(src.contains("dev.doctor4t.wathe.cca.GameWorldComponent"));
        assertTrue(src.contains("dev.doctor4t.wathe.game.GameFunctions"));
        assertTrue(src.contains("GameWorldComponent.KEY.get"));
        assertTrue(src.contains("game.getRole(player)"));
        assertTrue(src.contains("game.isRunning()"));
        assertTrue(src.contains("GameFunctions.isPlayerAliveAndSurvival(player)"));
        assertTrue(src.contains("NoisemakerEffectPolicy.shouldTrigger"));
    }

    @Test
    void noisemakerItemPlaysVanillaServerSoundAndAppliesMinecraftItemCooldown() throws IOException {
        String src = Files.readString(ITEM, StandardCharsets.UTF_8);

        assertTrue(src.contains("net.minecraft.sound.SoundEvents"));
        assertTrue(src.contains("SoundEvents.ITEM_GOAT_HORN_PLAY"));
        assertTrue(src.contains("SoundCategory.PLAYERS"));
        assertTrue(src.contains("world.playSound(null"));
        assertTrue(src.contains("getItemCooldownManager().isCoolingDown(StrawCraftItems.NOISEMAKER)"));
        assertTrue(src.contains("getItemCooldownManager().set(StrawCraftItems.NOISEMAKER"));
        assertTrue(src.contains("NoisemakerEffectPolicy.COOLDOWN_TICKS"));
    }

    @Test
    void noisemakerRuntimeDoesNotUseChatPacketsDamageIdentityOrSparkNoellesRuntime() throws IOException {
        String item = Files.readString(ITEM, StandardCharsets.UTF_8);
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);

        assertFalse(item.contains("sendMessage"));
        assertFalse(item.contains("Text.literal"));
        assertFalse(item.contains("PlaySoundS2CPacket"));
        assertFalse(item.contains("damage("));
        assertFalse(item.contains("addStatusEffect"));
        assertFalse(item.contains("setVelocity"));
        assertFalse(policy.contains("WinStatus"));

        for (Path path : mainJavaSources()) {
            String src = Files.readString(path, StandardCharsets.UTF_8);
            assertFalse(src.contains("import org.trainmurdermystery"), path + " must not import Spark Wathe runtime");
            assertFalse(src.contains("import org.noellesroles"), path + " must not import Spark NoellesRoles runtime");
            assertFalse(src.contains("XruiDD.NoellesRoles"), path + " must not depend on Spark NoellesRoles internals");
            assertFalse(src.contains("wathe-Parox"), path + " must not target Parox Wathe");
        }
    }

    private static List<Path> mainJavaSources() throws IOException {
        try (Stream<Path> sources = Files.walk(MAIN_ROOT)) {
            return sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }
}
