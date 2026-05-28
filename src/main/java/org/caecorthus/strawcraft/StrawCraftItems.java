package org.caecorthus.strawcraft;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class StrawCraftItems {
    public static final Identifier MASTER_KEY_ID = StrawCraft.id("master_key");
    public static final Identifier ANTIDOTE_ID = StrawCraft.id("antidote");
    public static final Identifier NOISEMAKER_ID = StrawCraft.id("noisemaker");

    public static Item MASTER_KEY;
    public static Item ANTIDOTE;
    public static Item NOISEMAKER;

    private StrawCraftItems() {
    }

    public static void register() {
        MASTER_KEY = Registry.register(Registries.ITEM, MASTER_KEY_ID, new Item(new Item.Settings().maxCount(1)));
        ANTIDOTE = Registry.register(Registries.ITEM, ANTIDOTE_ID, new ToxicologistAntidoteItem(new Item.Settings().maxCount(1)));
        NOISEMAKER = Registry.register(Registries.ITEM, NOISEMAKER_ID, new NoisemakerItem(new Item.Settings().maxCount(1)));
    }
}
