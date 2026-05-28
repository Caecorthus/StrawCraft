package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WaiterShopLoadout {
    static final String WAITER_SERVICE_ENTRY_ID = "waiter_service_tray";
    private static final int WAITER_SERVICE_PRICE = 50;

    private WaiterShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(WaiterShopLoadout::addWaiterServiceEntry);
    }

    static void addWaiterServiceEntry(StrawShopEvents.ShopContext context) {
        context.replaceEntries(withWaiterServiceEntry(context.getEntries()));
    }

    static List<ShopEntry> withWaiterServiceEntry(List<? extends ShopEntry> entries) {
        return withWaiterServiceEntry(entries, WaiterShopLoadout::serviceTrayStack);
    }

    static List<ShopEntry> withWaiterServiceEntry(List<? extends ShopEntry> entries, Supplier<ItemStack> stackFactory) {
        if (entries.stream().anyMatch(WaiterShopLoadout::isWaiterServiceEntry)) {
            return List.copyOf(entries);
        }

        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(waiterServiceEntry(stackFactory));
        return List.copyOf(nextEntries);
    }

    static boolean isWaiterServiceEntry(ShopEntry entry) {
        return WAITER_SERVICE_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    private static ItemStack serviceTrayStack() {
        ItemStack stack = StrawCraftItems.WAITER_SERVICE_TRAY.getDefaultStack();
        stack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + WAITER_SERVICE_ENTRY_ID)
        );
        return stack;
    }

    private static ShopEntry waiterServiceEntry(Supplier<ItemStack> stackFactory) {
        ItemStack stack = stackFactory.get();
        ShopEntry delivery = new ShopEntry(stack, WAITER_SERVICE_PRICE, ShopEntry.Type.TOOL);
        return new StrawShopEntry(
                WAITER_SERVICE_ENTRY_ID,
                stack,
                stack,
                WAITER_SERVICE_PRICE,
                ShopEntry.Type.TOOL,
                0,
                0,
                -1,
                delivery
        );
    }
}
