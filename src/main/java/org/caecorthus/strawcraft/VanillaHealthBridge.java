package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawKillEvents;

public final class VanillaHealthBridge {
    private VanillaHealthBridge() {
    }

    public static void registerKillRequestHandler() {
        StrawKillEvents.BEFORE_KILL.register((victim, killer, deathReason) -> {
            if (!(victim instanceof ServerPlayerEntity serverVictim)) {
                return StrawKillEvents.KillDecision.pass();
            }
            boolean knifeBackstab = killer != null && WeaponBalance.isBackstab(victim, killer);
            KillRequestPlan plan = planKillRequest(deathReason, knifeBackstab, killer != null);
            if (!plan.cancelsWatheKill()) {
                return StrawKillEvents.KillDecision.pass();
            }
            if (plan.appliesVanillaDamage()) {
                // Wathe weapons normally bypass hearts and move players straight to spectator.
                // StrawCraft turns those kill requests back into vanilla damage instead.
                // Wathe 武器通常会绕过红心，直接把玩家送进旁观。
                // StrawCraft 在这里把这些击杀请求改回原版伤害。
                WatheDeathReasonTracker.damageWithReason(serverVictim, deathReason, getDamageSource(serverVictim, killer, plan), plan.damage());
            }
            return StrawKillEvents.KillDecision.cancel();
        });
    }

    static KillRequestPlan planKillRequest(Identifier deathReason, boolean knifeBackstab, boolean hasKiller) {
        if (!shouldConvertWatheKillToVanillaDamage(deathReason)) {
            return new KillRequestPlan(0.0f, DamageSourceKind.GENERIC, false);
        }
        float damage = VanillaDamagePolicy.damageFor(deathReason, knifeBackstab);
        return new KillRequestPlan(
                damage,
                hasKiller ? DamageSourceKind.PLAYER_ATTACK : DamageSourceKind.GENERIC,
                true
        );
    }

    private static boolean shouldConvertWatheKillToVanillaDamage(Identifier deathReason) {
        return GameConstants.DeathReasons.KNIFE.equals(deathReason)
                || GameConstants.DeathReasons.GUN.equals(deathReason)
                || GameConstants.DeathReasons.BAT.equals(deathReason)
                || GameConstants.DeathReasons.GRENADE.equals(deathReason)
                || GameConstants.DeathReasons.POISON.equals(deathReason)
                || GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason)
                || StrawDeathReasons.MENTAL_BREAKDOWN.equals(deathReason);
    }

    private static DamageSource getDamageSource(ServerPlayerEntity victim, PlayerEntity killer, KillRequestPlan plan) {
        if (plan.damageSourceKind() == DamageSourceKind.PLAYER_ATTACK && killer != null) {
            return victim.getDamageSources().playerAttack(killer);
        }
        return victim.getDamageSources().generic();
    }

    enum DamageSourceKind {
        PLAYER_ATTACK,
        GENERIC
    }

    record KillRequestPlan(float damage, DamageSourceKind damageSourceKind, boolean cancelsWatheKill) {
        boolean appliesVanillaDamage() {
            return damage > 0.0f;
        }
    }
}
