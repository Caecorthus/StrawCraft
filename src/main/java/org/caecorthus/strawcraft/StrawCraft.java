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
        StrawCraftEntities.register();
        StrawCraftItems.register();
        NoellesRoleCatalog.registerWithWathe();
        StrawMapConfigReloader.register();
        StrawMapVoting.register();
        StrawInteractionBlacklistAdapter.register();
        WatheOfficialBridge.register();
        ConductorMasterKeyRuntime.register();
        EngineerRepairToolRuntime.register();
        StrawCorpseMetadata.registerEvents();
        ScavengerHiddenBodies.registerEvents();
        DetectiveKillHistoryRuntime.registerEvents();
        CoronerInspectionRuntime.register();
        VultureBodyFeastRuntime.register();
        ProfessorIronManProtectionRuntime.registerEvents();
        BodyguardProtectionRuntime.registerEvents();
        NoellesNeutralWinAdapter.registerEvents();
        SurvivalMasterCountdownRuntime.registerEvents();
        MermaidWaterAdaptationRuntime.registerEvents();
        ToxicologistPoisonVisibility.registerEvents();
        WeaponBalance.registerItemAttributes();
        RoleAssignedLoadouts.register();
        VanillaHealthBridge.registerKillRequestHandler();
        KillerShopLoadout.registerShopEntriesHandler();
        BanditShopLoadout.registerShopEntriesHandler();
        TimekeeperShopLoadout.registerShopEntriesHandler();
        ReporterShopLoadout.registerShopEntriesHandler();
        BartenderDefenseVialShopLoadout.registerShopEntriesHandler();
        WaiterShopLoadout.registerShopEntriesHandler();
        SilencerShopLoadout.registerShopEntriesHandler();
        PoisonerShopLoadout.registerShopEntriesHandler();
        WaiterServiceRuntime.register();
        PoisonerShopLoadout.registerPurchasePriceHandler();
        BanditShopLoadout.registerPurchasePriceHandler();
        BomberTimedBombRuntime.register();
        RecallerRecallRuntime.register();
        SwapperSwapRuntime.register();
        ReporterMarkRuntime.register();
        VoodooBondRuntime.register();
        PhantomInvisibilityRuntime.register();
        SpiritualistProjectionRuntime.register();
        PathogenInfectionRuntime.register();
        AssassinGuessRuntime.register();
        SerialKillerRuntime.register();
        DemonHunterPsychoRuntime.register();
        WatheOfficialBridge.rewriteGlobalShopEntries();
        TaczAmmoRefillTimers.register();
    }
}
