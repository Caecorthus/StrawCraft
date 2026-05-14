package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;

public final class VanillaDamagePolicy {
    private VanillaDamagePolicy() {
    }

    public static float damageFor(Identifier deathReason) {
        if (GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            return 8.0f;
        }
        if (GameConstants.DeathReasons.GUN.equals(deathReason)) {
            return 8.0f;
        }
        if (GameConstants.DeathReasons.GUN_BACKFIRE.equals(deathReason)) {
            return 4.0f;
        }
        if (GameConstants.DeathReasons.SHOT_INNOCENT.equals(deathReason)) {
            return 6.0f;
        }
        if (GameConstants.DeathReasons.BAT.equals(deathReason)) {
            return 6.0f;
        }
        if (GameConstants.DeathReasons.GRENADE.equals(deathReason)) {
            return 12.0f;
        }
        if (GameConstants.DeathReasons.POISON.equals(deathReason)) {
            return 4.0f;
        }
        if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason)) {
            return 20.0f;
        }
        if (GameConstants.DeathReasons.MENTAL_BREAKDOWN.equals(deathReason)) {
            return 4.0f;
        }
        return 0.0f;
    }
}
