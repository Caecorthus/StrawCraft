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

    public static Item MASTER_KEY;
    public static Item ANTIDOTE;
    public static Item DEFENSE_VIAL;
    public static Item NOISEMAKER;
    public static Item REPAIR_TOOL;
    public static Item TIMED_BOMB;

    private StrawCraftItems() {
    }

    public static void register() {
        MASTER_KEY = Registry.register(Registries.ITEM, MASTER_KEY_ID, new Item(new Item.Settings().maxCount(1)));
        ANTIDOTE = Registry.register(Registries.ITEM, ANTIDOTE_ID, new ToxicologistAntidoteItem(new Item.Settings().maxCount(1)));
        DEFENSE_VIAL = Registry.register(Registries.ITEM, DEFENSE_VIAL_ID, new BartenderDefenseVialItem(new Item.Settings().maxCount(1)));
        NOISEMAKER = Registry.register(Registries.ITEM, NOISEMAKER_ID, new NoisemakerItem(new Item.Settings().maxCount(1)));
        REPAIR_TOOL = Registry.register(Registries.ITEM, REPAIR_TOOL_ID, new EngineerRepairToolItem(new Item.Settings().maxCount(1)));
        TIMED_BOMB = Registry.register(Registries.ITEM, TIMED_BOMB_ID, new TimedBombItem(new Item.Settings().maxCount(1)));
    }
}
