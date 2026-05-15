package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class VanillaHealthBridge {
    private VanillaHealthBridge() {
    }

    public static void registerKillRequestHandler() {
        KillPlayer.BEFORE.register((victim, killer, deathReason) -> {
            boolean knifeBackstab = killer != null && WeaponBalance.isBackstab(victim, killer);
            KillRequestPlan plan = planKillRequest(deathReason, knifeBackstab, killer != null);
            if (plan.appliesVanillaDamage()) {
                // Wathe weapons normally bypass hearts and move players straight to spectator.
                // StrawCraft turns those kill requests back into vanilla damage instead.
                victim.damage(getDamageSource(victim, killer, plan), plan.damage());
            }
            return KillPlayer.KillResult.cancel();
        });
    }

    static KillRequestPlan planKillRequest(Identifier deathReason, boolean knifeBackstab, boolean hasKiller) {
        float damage = VanillaDamagePolicy.damageFor(deathReason, knifeBackstab);
        return new KillRequestPlan(
                damage,
                hasKiller ? DamageSourceKind.PLAYER_ATTACK : DamageSourceKind.GENERIC
        );
    }

    public static void markPlayerDeadAfterVanillaDeath(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (shouldMarkPlayerDeadAfterVanillaDeath(
                game.isRunning(),
                game.hasAnyRole(player.getUuid()),
                game.isPlayerDead(player.getUuid())
        )) {
            // Keep Wathe's win-condition bookkeeping aware of vanilla deaths.
            game.markPlayerDead(player.getUuid());
            game.sync();
        }
    }

    static boolean shouldMarkPlayerDeadAfterVanillaDeath(boolean watheRoundRunning, boolean hasAnyRole, boolean alreadyDead) {
        return watheRoundRunning && hasAnyRole && !alreadyDead;
    }

    private static DamageSource getDamageSource(ServerPlayerEntity victim, ServerPlayerEntity killer, KillRequestPlan plan) {
        if (plan.damageSourceKind() == DamageSourceKind.PLAYER_ATTACK && killer != null) {
            return victim.getDamageSources().playerAttack(killer);
        }
        return victim.getDamageSources().generic();
    }

    enum DamageSourceKind {
        PLAYER_ATTACK,
        GENERIC
    }

    record KillRequestPlan(float damage, DamageSourceKind damageSourceKind) {
        boolean cancelsWatheKill() {
            return true;
        }

        boolean appliesVanillaDamage() {
            return damage > 0.0f;
        }
    }
}
