package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public record ShopEntryViewState(
        int wathePurchaseIndex,
        ShopEntry.Type type,
        ItemStack displayStack,
        ItemStack actualStack,
        boolean active,
        String priceText,
        Optional<Status> cooldownStatus,
        Optional<Status> stockStatus
) {
    public static final int DEFAULT_STATUS_COLOR = 0xFFFFFF;
    public static final int UNAVAILABLE_STATUS_COLOR = 0xFFFF7777;

    private static final int TICKS_PER_SECOND = 20;

    public static ShopEntryViewState withoutShop(int price) {
        return withoutShop(-1, null, null, null, price);
    }

    public static ShopEntryViewState withoutShop(
            int wathePurchaseIndex,
            ShopEntry.Type type,
            ItemStack displayStack,
            ItemStack actualStack,
            int price
    ) {
        return new ShopEntryViewState(
                wathePurchaseIndex,
                type,
                displayStack,
                actualStack,
                true,
                String.valueOf(price),
                Optional.empty(),
                Optional.empty()
        );
    }

    public static ShopEntryViewState fromSnapshot(Snapshot snapshot) {
        return fromSnapshot(-1, null, null, null, snapshot);
    }

    public static ShopEntryViewState fromSnapshot(
            int wathePurchaseIndex,
            ShopEntry.Type type,
            ItemStack displayStack,
            ItemStack actualStack,
            Snapshot snapshot
    ) {
        boolean active = !snapshot.onCooldown() && snapshot.inStock();
        return new ShopEntryViewState(
                wathePurchaseIndex,
                type,
                displayStack,
                actualStack,
                active,
                String.valueOf(snapshot.price()),
                cooldownStatusFor(snapshot),
                stockStatusFor(snapshot)
        );
    }

    public Optional<Status> status() {
        return this.cooldownStatus.or(() -> this.stockStatus);
    }

    /**
     * Compatibility alias for UI/tests that still use the old presentation name.
     * 兼容旧 UI/测试使用的展示层命名；语义仍然是 Wathe 原始购买编号。
     */
    public int purchaseIndex() {
        return this.wathePurchaseIndex;
    }

    private static Optional<Status> cooldownStatusFor(Snapshot snapshot) {
        if (snapshot.onCooldown()) {
            int remainingSeconds = Math.max(1, snapshot.remainingCooldownTicks() / TICKS_PER_SECOND);
            return Optional.of(new Status(remainingSeconds + "s", UNAVAILABLE_STATUS_COLOR));
        }
        return Optional.empty();
    }

    private static Optional<Status> stockStatusFor(Snapshot snapshot) {
        if (snapshot.maxStock() > 0) {
            int color = snapshot.inStock() ? DEFAULT_STATUS_COLOR : UNAVAILABLE_STATUS_COLOR;
            return Optional.of(new Status(String.valueOf(snapshot.remainingStock()), color));
        }
        return Optional.empty();
    }

    public record Snapshot(
            int price,
            boolean onCooldown,
            int remainingCooldownTicks,
            int maxStock,
            int remainingStock,
            boolean inStock
    ) {
    }

    public record Status(String text, int color) {
    }
}
