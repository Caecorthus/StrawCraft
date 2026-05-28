package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.jetbrains.annotations.Nullable;

public final class PoisonNeedlePolicy {
    public static final int POISON_TICKS = 40 * 20;
    public static final int COOLDOWN_TICKS = 60 * 20;

    private PoisonNeedlePolicy() {
    }

    public static Decision evaluateUse(Input input) {
        if (!input.roundRunning()
                || !StrawRoleMeaning.usesPoisonerShop(input.role())
                || !input.userAliveAndSurvival()
                || !input.targetAlive()
                || !input.targetSurvival()
                || input.samePlayer()
                || input.targetPoisonTicks() > 0
                || !input.cooldownReady()) {
            return new Decision(false, Math.max(0, input.targetPoisonTicks()));
        }
        return new Decision(true, POISON_TICKS);
    }

    public record Input(
            @Nullable Role role,
            boolean roundRunning,
            boolean userAliveAndSurvival,
            boolean targetAlive,
            boolean targetSurvival,
            boolean samePlayer,
            int targetPoisonTicks,
            boolean cooldownReady
    ) {
    }

    public record Decision(boolean allowed, int poisonTicksAfterUse) {
    }
}
