package org.caecorthus.strawcraft.client;

import java.util.Optional;

public record ShopEntryViewState(
        boolean active,
        String priceText,
        Optional<Status> status
) {
    public static final int DEFAULT_STATUS_COLOR = 0xFFFFFF;
    public static final int UNAVAILABLE_STATUS_COLOR = 0xFFFF7777;

    private static final int TICKS_PER_SECOND = 20;

    public static ShopEntryViewState withoutShop(int price) {
        return new ShopEntryViewState(true, String.valueOf(price), Optional.empty());
    }

    public static ShopEntryViewState fromSnapshot(Snapshot snapshot) {
        boolean active = !snapshot.onCooldown() && snapshot.inStock();
        Optional<Status> status = statusFor(snapshot);
        return new ShopEntryViewState(active, String.valueOf(snapshot.price()), status);
    }

    private static Optional<Status> statusFor(Snapshot snapshot) {
        if (snapshot.onCooldown()) {
            int remainingSeconds = Math.max(1, snapshot.remainingCooldownTicks() / TICKS_PER_SECOND);
            return Optional.of(new Status(remainingSeconds + "s", UNAVAILABLE_STATUS_COLOR));
        }
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
