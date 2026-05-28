package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class BartenderDefenseVialShopLoadout {
    static final String DEFENSE_VIAL_ENTRY_ID = "defense_vial";
    private static final int DEFENSE_VIAL_PRICE = 100;

    private BartenderDefenseVialShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(BartenderDefenseVialShopLoadout::addDefenseVialEntry);
    }

    static void addDefenseVialEntry(StrawShopEvents.ShopContext context) {
        context.replaceEntries(withDefenseVialEntry(context.getEntries()));
    }

    static List<ShopEntry> withDefenseVialEntry(List<? extends ShopEntry> entries) {
        return withDefenseVialEntry(entries, BartenderDefenseVialShopLoadout::defenseVialStack);
    }

    static List<ShopEntry> withDefenseVialEntry(
            List<? extends ShopEntry> entries,
            Supplier<ItemStack> stackFactory
    ) {
        if (entries.stream().anyMatch(BartenderDefenseVialShopLoadout::isDefenseVialEntry)) {
            return List.copyOf(entries);
        }
        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(defenseVialEntry(stackFactory));
        return List.copyOf(nextEntries);
    }

    static boolean isDefenseVialEntry(ShopEntry entry) {
        return DEFENSE_VIAL_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    private static ItemStack defenseVialStack() {
        ItemStack vialStack = StrawCraftItems.DEFENSE_VIAL.getDefaultStack();
        vialStack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + DEFENSE_VIAL_ENTRY_ID)
        );
        return vialStack;
    }

    private static ShopEntry defenseVialEntry(Supplier<ItemStack> stackFactory) {
        ItemStack vialStack = stackFactory.get();
        ShopEntry delivery = new ShopEntry(vialStack, DEFENSE_VIAL_PRICE, ShopEntry.Type.TOOL);
        return new StrawShopEntry(
                DEFENSE_VIAL_ENTRY_ID,
                vialStack,
                vialStack,
                DEFENSE_VIAL_PRICE,
                ShopEntry.Type.TOOL,
                0,
                0,
                -1,
                delivery
        );
    }
}
