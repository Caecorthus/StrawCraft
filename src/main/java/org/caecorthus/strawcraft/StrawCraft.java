package org.caecorthus.strawcraft;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.map.StrawInteractionBlacklistAdapter;
import org.caecorthus.strawcraft.map.StrawMapConfigReloader;
import org.caecorthus.strawcraft.map.StrawMapVoting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrawCraft implements ModInitializer {
    public static final String MOD_ID = "strawcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        StrawCraftItems.register();
        NoellesRoleCatalog.registerWithWathe();
        StrawMapConfigReloader.register();
        StrawMapVoting.register();
        StrawInteractionBlacklistAdapter.register();
        WatheOfficialBridge.register();
        ConductorMasterKeyRuntime.register();
        StrawCorpseMetadata.registerEvents();
        ScavengerHiddenBodies.registerEvents();
        DetectiveKillHistoryRuntime.registerEvents();
        VultureBodyFeastRuntime.register();
        ProfessorIronManProtectionRuntime.registerEvents();
        NoellesNeutralWinAdapter.registerEvents();
        ToxicologistPoisonVisibility.registerEvents();
        WeaponBalance.registerItemAttributes();
        RoleAssignedLoadouts.register();
        VanillaHealthBridge.registerKillRequestHandler();
        KillerShopLoadout.registerShopEntriesHandler();
        TimekeeperShopLoadout.registerShopEntriesHandler();
        ReporterShopLoadout.registerShopEntriesHandler();
        WatheOfficialBridge.rewriteGlobalShopEntries();
        TaczAmmoRefillTimers.register();
    }
}
