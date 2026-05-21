package org.caecorthus.strawcraft;

import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;

public final class StrawPlayerShopState {
    private static final String COOLDOWNS_KEY = "CooldownDeadlines";
    private static final String STOCK_KEY = "StockRemaining";

    private final Map<String, Long> cooldownDeadlines = new HashMap<>();
    private final Map<String, Integer> stockRemaining = new HashMap<>();

    public void reset() {
        cooldownDeadlines.clear();
        stockRemaining.clear();
    }

    public void ensureEntry(StrawShopEntry entry, long now) {
        if (entry.hasStockLimit()) {
            stockRemaining.putIfAbsent(entry.id(), entry.maxStock());
        }
        if (entry.initialCooldownTicks() > 0) {
            cooldownDeadlines.putIfAbsent(entry.id(), now + entry.initialCooldownTicks());
        }
    }

    public boolean canPurchase(StrawShopEntry entry, long now) {
        ensureEntry(entry, now);
        return !isOnCooldown(entry.id(), now) && isInStock(entry.id());
    }

    public void recordPurchase(StrawShopEntry entry, long now) {
        if (entry.hasStockLimit()) {
            stockRemaining.compute(entry.id(), (id, remaining) -> Math.max(0, (remaining == null ? entry.maxStock() : remaining) - 1));
        }
        if (entry.cooldownTicks() > 0) {
            cooldownDeadlines.put(entry.id(), now + entry.cooldownTicks());
        }
    }

    public boolean isOnCooldown(String entryId, long now) {
        return getRemainingCooldown(entryId, now) > 0;
    }

    public int getRemainingCooldown(String entryId, long now) {
        long deadline = cooldownDeadlines.getOrDefault(entryId, 0L);
        long remaining = Math.max(0L, deadline - now);
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    public int getRemainingStock(String entryId) {
        return stockRemaining.getOrDefault(entryId, -1);
    }

    public boolean isInStock(String entryId) {
        return !stockRemaining.containsKey(entryId) || stockRemaining.get(entryId) > 0;
    }

    public void readFromNbt(NbtCompound nbt) {
        cooldownDeadlines.clear();
        stockRemaining.clear();

        NbtCompound cooldowns = nbt.getCompound(COOLDOWNS_KEY);
        for (String entryId : cooldowns.getKeys()) {
            cooldownDeadlines.put(entryId, cooldowns.getLong(entryId));
        }

        NbtCompound stock = nbt.getCompound(STOCK_KEY);
        for (String entryId : stock.getKeys()) {
            stockRemaining.put(entryId, stock.getInt(entryId));
        }
    }

    public void writeToNbt(NbtCompound nbt) {
        NbtCompound cooldowns = new NbtCompound();
        cooldownDeadlines.forEach(cooldowns::putLong);
        nbt.put(COOLDOWNS_KEY, cooldowns);

        NbtCompound stock = new NbtCompound();
        stockRemaining.forEach(stock::putInt);
        nbt.put(STOCK_KEY, stock);
    }
}
