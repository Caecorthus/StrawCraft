package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class TimekeeperShopLoadout {
    static final String TIME_SUBTRACTION_ENTRY_ID = "timekeeper_subtract_time";
    private static final int TIME_SUBTRACTION_PRICE = 150;
    private static final int TIME_SUBTRACTION_TICKS = 600;

    private TimekeeperShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(TimekeeperShopLoadout::addTimeSubtractionEntry);
    }

    static void addTimeSubtractionEntry(StrawShopEvents.ShopContext ctx) {
        ctx.replaceEntries(withTimeSubtractionEntry(ctx.getEntries()));
    }

    static List<ShopEntry> withTimeSubtractionEntry(List<ShopEntry> entries) {
        return withTimeSubtractionEntry(entries, TimekeeperShopLoadout::clockDisplayStack);
    }

    static List<ShopEntry> withTimeSubtractionEntry(List<ShopEntry> entries, Supplier<ItemStack> displayStackFactory) {
        if (entries.stream().anyMatch(TimekeeperShopLoadout::isTimeSubtractionEntry)) {
            return List.copyOf(entries);
        }
        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(timeSubtractionEntry(displayStackFactory));
        return List.copyOf(nextEntries);
    }

    static boolean isTimeSubtractionEntry(ShopEntry entry) {
        return TIME_SUBTRACTION_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    static boolean subtractTime(IntConsumer timerAdjustment) {
        timerAdjustment.accept(-TIME_SUBTRACTION_TICKS);
        return true;
    }

    private static ItemStack clockDisplayStack() {
        ItemStack displayStack = Items.CLOCK.getDefaultStack();
        displayStack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + TIME_SUBTRACTION_ENTRY_ID)
        );
        return displayStack;
    }

    private static ShopEntry timeSubtractionEntry(Supplier<ItemStack> displayStackFactory) {
        ItemStack displayStack = displayStackFactory.get();
        ShopEntry timerBehavior = new ShopEntry(displayStack, TIME_SUBTRACTION_PRICE, ShopEntry.Type.TOOL) {
            @Override
            public boolean onBuy(PlayerEntity player) {
                if (player == null) {
                    return false;
                }

                // Official Wathe owns the round timer; this slice only applies a small negative delta.
                // 官方 Wathe 仍然拥有回合计时器；这个切片只施加一个小的负向时间增量。
                return subtractTime(ticks -> GameTimeComponent.KEY.get(player.getWorld()).addTime(ticks));
            }
        };
        return new StrawShopEntry(
                TIME_SUBTRACTION_ENTRY_ID,
                displayStack,
                displayStack,
                TIME_SUBTRACTION_PRICE,
                ShopEntry.Type.TOOL,
                0,
                0,
                -1,
                timerBehavior
        );
    }
}
