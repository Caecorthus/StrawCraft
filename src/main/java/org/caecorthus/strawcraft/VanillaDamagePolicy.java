package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;

public final class VanillaDamagePolicy {
    private VanillaDamagePolicy() {
    }

    public static float damageFor(Identifier deathReason) {
        return damageFor(deathReason, false);
    }

    public static float damageFor(Identifier deathReason, boolean knifeBackstab) {
        if (GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            return knifeBackstab ? WeaponBalance.KNIFE_BACKSTAB_DAMAGE : WeaponBalance.KNIFE_FRONT_STAB_DAMAGE;
        }
        if (GameConstants.DeathReasons.GUN.equals(deathReason)) {
            return 0.0f;
        }
        if (GameConstants.DeathReasons.GUN_BACKFIRE.equals(deathReason)) {
            return 0.0f;
        }
        if (GameConstants.DeathReasons.SHOT_INNOCENT.equals(deathReason)) {
            return 0.0f;
        }
        if (GameConstants.DeathReasons.BAT.equals(deathReason)) {
            return WeaponBalance.BAT_ATTACK_DAMAGE;
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
