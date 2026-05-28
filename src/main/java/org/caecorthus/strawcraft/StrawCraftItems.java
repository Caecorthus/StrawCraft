package org.caecorthus.strawcraft;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class StrawCraftItems {
    public static final Identifier MASTER_KEY_ID = StrawCraft.id("master_key");
    public static final Identifier ANTIDOTE_ID = StrawCraft.id("antidote");
    public static final Identifier DEFENSE_VIAL_ID = StrawCraft.id("defense_vial");
    public static final Identifier NOISEMAKER_ID = StrawCraft.id("noisemaker");
    public static final Identifier REPAIR_TOOL_ID = StrawCraft.id("repair_tool");
    public static final Identifier TIMED_BOMB_ID = StrawCraft.id("timed_bomb");
    public static final Identifier WAITER_SERVICE_TRAY_ID = StrawCraft.id("waiter_service_tray");
    public static final Identifier POISON_NEEDLE_ID = StrawCraft.id("poison_needle");
    public static final Identifier DEMON_HUNTER_PISTOL_ID = StrawCraft.id("demon_hunter_pistol");
    public static final Identifier THROWING_AXE_ID = StrawCraft.id("throwing_axe");

    public static Item MASTER_KEY;
    public static Item ANTIDOTE;
    public static Item DEFENSE_VIAL;
    public static Item NOISEMAKER;
    public static Item REPAIR_TOOL;
    public static Item TIMED_BOMB;
    public static Item WAITER_SERVICE_TRAY;
    public static Item POISON_NEEDLE;
    public static Item DEMON_HUNTER_PISTOL;
    public static Item THROWING_AXE;

    private StrawCraftItems() {
    }

    public static void register() {
        MASTER_KEY = Registry.register(Registries.ITEM, MASTER_KEY_ID, new Item(new Item.Settings().maxCount(1)));
        ANTIDOTE = Registry.register(Registries.ITEM, ANTIDOTE_ID, new ToxicologistAntidoteItem(new Item.Settings().maxCount(1)));
        DEFENSE_VIAL = Registry.register(Registries.ITEM, DEFENSE_VIAL_ID, new BartenderDefenseVialItem(new Item.Settings().maxCount(1)));
        NOISEMAKER = Registry.register(Registries.ITEM, NOISEMAKER_ID, new NoisemakerItem(new Item.Settings().maxCount(1)));
        REPAIR_TOOL = Registry.register(Registries.ITEM, REPAIR_TOOL_ID, new EngineerRepairToolItem(new Item.Settings().maxCount(1)));
        TIMED_BOMB = Registry.register(Registries.ITEM, TIMED_BOMB_ID, new TimedBombItem(new Item.Settings().maxCount(1)));
        WAITER_SERVICE_TRAY = Registry.register(Registries.ITEM, WAITER_SERVICE_TRAY_ID, new WaiterServiceItem(new Item.Settings().maxCount(1)));
        POISON_NEEDLE = Registry.register(Registries.ITEM, POISON_NEEDLE_ID, new PoisonNeedleItem(new Item.Settings().maxCount(1)));
        DEMON_HUNTER_PISTOL = Registry.register(Registries.ITEM, DEMON_HUNTER_PISTOL_ID, new DemonHunterPistolItem(new Item.Settings().maxCount(1)));
        THROWING_AXE = Registry.register(Registries.ITEM, THROWING_AXE_ID, new ThrowingAxeItem(new Item.Settings().maxCount(1)));
    }
}
