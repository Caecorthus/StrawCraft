package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ReporterShopLoadout {
    static final String REPORTER_NOTE_ENTRY_ID = "reporter_note";
    private static final int REPORTER_NOTE_PRICE = 25;

    private ReporterShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(ReporterShopLoadout::addReporterNoteEntry);
    }

    static void addReporterNoteEntry(StrawShopEvents.ShopContext ctx) {
        ctx.replaceEntries(withReporterNoteEntry(ctx.getEntries()));
    }

    static List<ShopEntry> withReporterNoteEntry(List<? extends ShopEntry> entries) {
        return withReporterNoteEntry(entries, ReporterShopLoadout::noteDisplayStack);
    }

    static List<ShopEntry> withReporterNoteEntry(
            List<? extends ShopEntry> entries,
            Supplier<ItemStack> displayStackFactory
    ) {
        if (entries.stream().anyMatch(ReporterShopLoadout::isReporterNoteEntry)) {
            return List.copyOf(entries);
        }
        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(reporterNoteEntry(displayStackFactory));
        return List.copyOf(nextEntries);
    }

    static boolean isReporterNoteEntry(ShopEntry entry) {
        return REPORTER_NOTE_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    private static ItemStack noteDisplayStack() {
        ItemStack noteStack = WatheItems.NOTE.getDefaultStack();
        noteStack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + REPORTER_NOTE_ENTRY_ID)
        );
        return noteStack;
    }

    private static ShopEntry reporterNoteEntry(Supplier<ItemStack> displayStackFactory) {
        ItemStack noteStack = displayStackFactory.get();
        // Delegate to official Wathe's normal item delivery; Reporter only gets shop access through role projection.
        // 委托官方 Wathe 的普通物品交付；记者只通过职业投影获得商店访问权。
        ShopEntry officialNoteDelivery = new ShopEntry(noteStack, REPORTER_NOTE_PRICE, ShopEntry.Type.TOOL);
        return new StrawShopEntry(
                REPORTER_NOTE_ENTRY_ID,
                noteStack,
                noteStack,
                REPORTER_NOTE_PRICE,
                ShopEntry.Type.TOOL,
                0,
                0,
                -1,
                officialNoteDelivery
        );
    }
}
