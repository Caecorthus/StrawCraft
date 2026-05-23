package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Optional;

public final class StrawShopEntry extends ShopEntry {
    private final String id;
    private final ItemStack displayStack;
    private final ItemStack actualStack;
    private final int cooldownTicks;
    private final int initialCooldownTicks;
    private final int maxStock;
    private final ShopEntry delegate;

    public StrawShopEntry(
            String id,
            ItemStack displayStack,
            ItemStack actualStack,
            int price,
            Type type,
            int cooldownTicks,
            int initialCooldownTicks,
            int maxStock
    ) {
        this(id, displayStack, actualStack, price, type, cooldownTicks, initialCooldownTicks, maxStock, null);
    }

    public StrawShopEntry(
            String id,
            ItemStack displayStack,
            ItemStack actualStack,
            int price,
            Type type,
            int cooldownTicks,
            int initialCooldownTicks,
            int maxStock,
            ShopEntry delegate
    ) {
        super(displayStack, price, type);
        this.id = id;
        this.displayStack = displayStack;
        this.actualStack = actualStack == null ? displayStack : actualStack;
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.initialCooldownTicks = Math.max(0, initialCooldownTicks);
        this.maxStock = maxStock;
        this.delegate = delegate;
    }

    @Override
    public boolean onBuy(PlayerEntity player) {
        if (player == null || delegate == null && (actualStack == null || actualStack.isEmpty())) {
            return false;
        }
        return delegate == null
                ? ShopEntry.insertStackInFreeSlot(player, actualStack.copy())
                : delegate.onBuy(player);
    }

    public String id() {
        return id;
    }

    public ItemStack displayStack() {
        return displayStack;
    }

    public ItemStack actualStack() {
        return actualStack;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int initialCooldownTicks() {
        return initialCooldownTicks;
    }

    public int maxStock() {
        return maxStock;
    }

    public boolean hasStockLimit() {
        return maxStock >= 0;
    }

    public boolean requiresShopAccess() {
        return delegate == null;
    }

    public static ShopEntry decorate(ShopEntry entry) {
        if (entry instanceof StrawShopEntry) {
            return entry;
        }
        return new StrawShopEntry(
                fallbackId(entry),
                entry.stack(),
                entry.stack(),
                entry.price(),
                entry.type(),
                0,
                0,
                -1,
                entry
        );
    }

    public static Optional<StrawShopEntry> metadata(ShopEntry entry) {
        return entry instanceof StrawShopEntry strawEntry ? Optional.of(strawEntry) : Optional.empty();
    }

    public static String idFor(ShopEntry entry) {
        return metadata(entry).map(StrawShopEntry::id).orElseGet(() -> fallbackId(entry));
    }

    public static ItemStack displayStackFor(ShopEntry entry) {
        return metadata(entry).map(StrawShopEntry::displayStack).orElseGet(entry::stack);
    }

    public static ItemStack actualStackFor(ShopEntry entry) {
        return metadata(entry).map(StrawShopEntry::actualStack).orElseGet(entry::stack);
    }

    private static String fallbackId(ShopEntry entry) {
        ItemStack stack = entry.stack();
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()).toString();
    }
}
