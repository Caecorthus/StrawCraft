package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ScavengerShopLoadout {
    static final String RESET_KNIFE_COOLDOWN_ID = "scavenger_reset_knife_cooldown";
    private static final int RESET_KNIFE_COOLDOWN_PRICE = 150;

    private ScavengerShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(ScavengerShopLoadout::addResetKnifeCooldownEntry);
    }

    static void addResetKnifeCooldownEntry(StrawShopEvents.ShopContext context) {
        context.replaceEntries(withResetKnifeCooldownEntry(context.getEntries()));
    }

    static List<ShopEntry> withResetKnifeCooldownEntry(List<ShopEntry> entries) {
        return withResetKnifeCooldownEntry(entries, ScavengerShopLoadout::clockDisplayStack);
    }

    static List<ShopEntry> withResetKnifeCooldownEntry(List<ShopEntry> entries, Supplier<ItemStack> displayStackFactory) {
        if (entries.stream().anyMatch(ScavengerShopLoadout::isResetKnifeCooldownEntry)) {
            return List.copyOf(entries);
        }

        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(resetKnifeCooldownEntry(displayStackFactory));
        return List.copyOf(nextEntries);
    }

    static boolean isResetKnifeCooldownEntry(ShopEntry entry) {
        return RESET_KNIFE_COOLDOWN_ID.equals(StrawShopEntry.idFor(entry));
    }

    static void resetKnifeCooldown(ItemCooldownManager cooldowns) {
        cooldowns.remove(WatheItems.KNIFE);
    }

    private static ItemStack clockDisplayStack() {
        ItemStack displayStack = Items.CLOCK.getDefaultStack();
        displayStack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + RESET_KNIFE_COOLDOWN_ID)
        );
        return displayStack;
    }

    private static ShopEntry resetKnifeCooldownEntry(Supplier<ItemStack> displayStackFactory) {
        ItemStack displayStack = displayStackFactory.get();
        ShopEntry resetBehavior = new ShopEntry(displayStack, RESET_KNIFE_COOLDOWN_PRICE, ShopEntry.Type.WEAPON) {
            @Override
            public boolean onBuy(PlayerEntity player) {
                if (player == null) {
                    return false;
                }

                // This is a real Minecraft cooldown mutation, not a placeholder item delivery.
                // 这里直接修改 Minecraft 物品冷却，不用占位物品假装实现效果。
                resetKnifeCooldown(player.getItemCooldownManager());
                return true;
            }
        };
        return new StrawShopEntry(
                RESET_KNIFE_COOLDOWN_ID,
                displayStack,
                displayStack,
                RESET_KNIFE_COOLDOWN_PRICE,
                ShopEntry.Type.WEAPON,
                0,
                0,
                -1,
                resetBehavior
        );
    }

}
