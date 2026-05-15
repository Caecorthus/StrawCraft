package org.caecorthus.strawcraft;

import net.fabricmc.api.ModInitializer;

public final class StrawCraft implements ModInitializer {
    public static final String MOD_ID = "strawcraft";

    @Override
    public void onInitialize() {
        WeaponBalance.registerItemAttributes();
        RoleAssignedLoadouts.register();
        VanillaHealthBridge.registerKillRequestHandler();
        KillerShopLoadout.registerShopEntriesHandler();
        TaczAmmoRefillTimers.register();
    }
}
