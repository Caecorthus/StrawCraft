package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

public final class NoisemakerEffectPolicy {
    public static final Identifier NOISEMAKER_ROLE = StrawCraft.id("noisemaker");
    public static final int COOLDOWN_TICKS = 20 * 10;

    private NoisemakerEffectPolicy() {
    }

    public static boolean shouldTrigger(Input input) {
        return input != null
                && !input.clientWorld()
                && input.roundRunning()
                && input.playerAlive()
                && input.cooldownReady()
                && StrawRoleMeaning.matchesRoleId(input.role(), NOISEMAKER_ROLE);
    }

    public record Input(
            boolean clientWorld,
            boolean roundRunning,
            Role role,
            boolean playerAlive,
            boolean cooldownReady
    ) {
    }
}
