package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SilencerShopLoadout {
    static final String SILENT_PSYCHO_ENTRY_ID = "silencer_silent_psycho_mode";
    static final int SILENT_PSYCHO_PRICE = 350;

    private SilencerShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(SilencerShopLoadout::addSilentPsychoEntry);
    }

    static void addSilentPsychoEntry(StrawShopEvents.ShopContext ctx) {
        ctx.replaceEntries(withSilentPsychoEntry(ctx.getEntries()));
    }

    static List<ShopEntry> withSilentPsychoEntry(List<? extends ShopEntry> entries) {
        return withSilentPsychoEntry(entries, SilencerShopLoadout::silentPsychoStack);
    }

    static List<ShopEntry> withSilentPsychoEntry(List<? extends ShopEntry> entries, Supplier<ItemStack> displayStackFactory) {
        if (entries.stream().anyMatch(SilencerShopLoadout::isSilentPsychoEntry)) {
            return List.copyOf(entries);
        }

        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(silentPsychoEntry(displayStackFactory));
        return List.copyOf(nextEntries);
    }

    static PlayerShopCatalog.Presentation presentation(List<ShopEntry> materializedEntries) {
        List<PlayerShopCatalog.VisibleEntry> visibleEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (isSilentPsychoEntry(entry)) {
                visibleEntries.add(new PlayerShopCatalog.VisibleEntry(index, entry));
            }
        }
        return new PlayerShopCatalog.Presentation(visibleEntries);
    }

    static boolean isSilentPsychoEntry(ShopEntry entry) {
        return SILENT_PSYCHO_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    static boolean startSilentPsycho(SilentPsychoModeAccess psychoMode) {
        return psychoMode != null && psychoMode.strawcraft$startSilentPsycho();
    }

    private static boolean startSilentPsycho(PlayerEntity player) {
        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(player);
        if (!(component instanceof SilentPsychoModeAccess psychoMode)) {
            return false;
        }
        return startSilentPsycho(psychoMode);
    }

    private static ItemStack silentPsychoStack() {
        ItemStack stack = WatheItems.PSYCHO_MODE.getDefaultStack();
        stack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + SILENT_PSYCHO_ENTRY_ID)
        );
        return stack;
    }

    private static ShopEntry silentPsychoEntry(Supplier<ItemStack> displayStackFactory) {
        ItemStack displayStack = displayStackFactory.get();
        ShopEntry delivery = new ShopEntry(displayStack, SILENT_PSYCHO_PRICE, ShopEntry.Type.POISON) {
            @Override
            public boolean onBuy(PlayerEntity player) {
                if (player == null) {
                    return false;
                }
                return SilencerShopLoadout.startSilentPsycho(player);
            }
        };
        return new StrawShopEntry(
                SILENT_PSYCHO_ENTRY_ID,
                displayStack,
                displayStack,
                SILENT_PSYCHO_PRICE,
                ShopEntry.Type.POISON,
                0,
                0,
                -1,
                delivery
        );
    }
}
