package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilencerPsychoModeContractTest {
    @Test
    void officialWathePsychoComponentDoesNotProvideSilentStartContract() {
        assertTrue(hasMethod("startPsycho"));
        assertFalse(hasMethod("startPsycho", boolean.class));
        assertTrue(hasMethod("stopPsycho"));
        assertFalse(hasMethod("stopPsycho", boolean.class));
    }

    @Test
    void addonMixinOwnsSilentPsychoCounterSafety() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/strawcraft.mixins.json"), StandardCharsets.UTF_8);
        String mixin = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/mixin/PlayerPsychoComponentMixin.java"),
                StandardCharsets.UTF_8
        );
        String loadout = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/SilencerShopLoadout.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(config.contains("\"PlayerPsychoComponentMixin\""));
        assertTrue(mixin.contains("implements SilentPsychoModeAccess"));
        assertTrue(mixin.contains("ShopEntry.insertStackInFreeSlot"));
        assertTrue(mixin.contains("setPsychoTicks(GameConstants.PSYCHO_TIMER)"));
        assertTrue(mixin.contains("setArmour(1)"));
        assertTrue(mixin.contains("if (!strawcraft$silentPsycho)"));
        assertTrue(mixin.contains("callback.cancel()"));
        assertTrue(mixin.contains("require = 0"));
        assertFalse(mixin.contains("setPsychosActive"));
        assertTrue(loadout.contains("component instanceof SilentPsychoModeAccess"));
        assertFalse(loadout.contains("startPsycho(false)"));
    }

    private static boolean hasMethod(String name, Class<?>... parameterTypes) {
        return Arrays.stream(PlayerPsychoComponent.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals(name)
                        && Arrays.equals(method.getParameterTypes(), parameterTypes));
    }
}
