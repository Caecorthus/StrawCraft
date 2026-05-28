package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoisemakerEffectPolicyTest {
    @Test
    void allowsAliveNoisemakerOnServerDuringRunningRoundWithCooldownReady() {
        assertTrue(NoisemakerEffectPolicy.shouldTrigger(new NoisemakerEffectPolicy.Input(
                false,
                true,
                noellesRole("noisemaker"),
                true,
                true
        )));
    }

    @Test
    void deniesWrongRoleDeadClientNullNonRunningAndCooldownBlockedContexts() {
        Stream.of(
                new NoisemakerEffectPolicy.Input(false, true, noellesRole("conductor"), true, true),
                new NoisemakerEffectPolicy.Input(false, true, noellesRole("noisemaker"), false, true),
                new NoisemakerEffectPolicy.Input(true, true, noellesRole("noisemaker"), true, true),
                new NoisemakerEffectPolicy.Input(false, true, null, true, true),
                new NoisemakerEffectPolicy.Input(false, false, noellesRole("noisemaker"), true, true),
                new NoisemakerEffectPolicy.Input(false, true, noellesRole("noisemaker"), true, false)
        ).forEach(input -> assertFalse(NoisemakerEffectPolicy.shouldTrigger(input), input.toString()));

        assertFalse(NoisemakerEffectPolicy.shouldTrigger(null));
    }

    private static Role noellesRole(String path) {
        return NoellesRoleCatalog.find(StrawCraft.id(path)).orElseThrow().watheRole();
    }
}
