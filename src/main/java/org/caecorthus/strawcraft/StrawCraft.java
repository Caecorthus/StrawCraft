package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.event.KillPlayer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class StrawCraft implements ModInitializer {
    public static final String MOD_ID = "strawcraft";

    @Override
    public void onInitialize() {
        WeaponBalance.registerItemAttributes();
        VigilanteLoadout.registerRoleAssignedHandler();
        registerVanillaHealthBridge();
        KillerShopLoadout.registerShopEntriesHandler();
        TaczAmmoRefillTimers.register();
    }

    private static void registerVanillaHealthBridge() {
        KillPlayer.BEFORE.register((victim, killer, deathReason) -> {
            boolean knifeBackstab = killer != null && WeaponBalance.isBackstab(victim, killer);
            float damage = VanillaDamagePolicy.damageFor(deathReason, knifeBackstab);
            if (damage <= 0.0f) {
                return KillPlayer.KillResult.cancel();
            }

            // Wathe weapons normally bypass hearts and move players straight to spectator.
            // StrawCraft turns those kill requests back into vanilla damage instead.
            victim.damage(getDamageSource(victim, killer), damage);
            return KillPlayer.KillResult.cancel();
        });
    }

    private static DamageSource getDamageSource(ServerPlayerEntity victim, ServerPlayerEntity killer) {
        if (killer != null) {
            return victim.getDamageSources().playerAttack(killer);
        }
        return victim.getDamageSources().generic();
    }
}
