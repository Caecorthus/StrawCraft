package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import dev.doctor4t.wathe.util.StoreBuyPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public final class WatheShopClientAdapter {
    public boolean canOpen(MinecraftClient client) {
        ClientPlayerEntity player = player(client);
        if (player == null) {
            return false;
        }
        return !entriesFor(player).isEmpty();
    }

    public ShopSnapshot snapshot(MinecraftClient client) {
        ClientPlayerEntity player = player(client);
        if (player == null) {
            return ShopSnapshot.empty();
        }
        return snapshotFrom(entriesFor(player), new PlayerShopState(PlayerShopComponent.KEY.get(player)));
    }

    public void buy(int index) {
        // Wathe revalidates the index server-side before completing the purchase.
        ClientPlayNetworking.send(new StoreBuyPayload(index));
    }

    public static ShopSnapshot snapshotFrom(List<ShopEntry> entries, ShopState shopState) {
        List<ShopEntryViewState> entryStates = new ArrayList<>(entries.size());
        for (ShopEntry entry : entries) {
            entryStates.add(viewStateFor(entry, shopState));
        }
        OptionalInt balance = shopState == null ? OptionalInt.empty() : shopState.balance();
        return new ShopSnapshot(List.copyOf(entries), balance, List.copyOf(entryStates));
    }

    private static ShopEntryViewState viewStateFor(ShopEntry entry, ShopState shopState) {
        if (shopState == null) {
            return ShopEntryViewState.withoutShop(entry.price());
        }
        return ShopEntryViewState.fromSnapshot(new ShopEntryViewState.Snapshot(
                entry.price(),
                shopState.isOnCooldown(entry.id()),
                shopState.remainingCooldownTicks(entry.id()),
                shopState.maxStock(entry.id()),
                shopState.remainingStock(entry.id()),
                shopState.isInStock(entry.id())
        ));
    }

    private static ClientPlayerEntity player(MinecraftClient client) {
        return client == null ? null : client.player;
    }

    private static List<ShopEntry> entriesFor(ClientPlayerEntity player) {
        try {
            return ShopUtils.getShopEntriesForPlayer(player);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public interface ShopState {
        OptionalInt balance();

        boolean isOnCooldown(String entryId);

        int remainingCooldownTicks(String entryId);

        int maxStock(String entryId);

        int remainingStock(String entryId);

        boolean isInStock(String entryId);
    }

    private record PlayerShopState(PlayerShopComponent shop) implements ShopState {
        @Override
        public OptionalInt balance() {
            return OptionalInt.of(shop.getBalance());
        }

        @Override
        public boolean isOnCooldown(String entryId) {
            return shop.isOnCooldown(entryId);
        }

        @Override
        public int remainingCooldownTicks(String entryId) {
            return shop.getRemainingCooldown(entryId);
        }

        @Override
        public int maxStock(String entryId) {
            return shop.getMaxStock(entryId);
        }

        @Override
        public int remainingStock(String entryId) {
            return shop.getRemainingStock(entryId);
        }

        @Override
        public boolean isInStock(String entryId) {
            return shop.isInStock(entryId);
        }
    }

    public record ShopSnapshot(
            List<ShopEntry> entries,
            OptionalInt balance,
            List<ShopEntryViewState> entryStates
    ) {
        public static ShopSnapshot empty() {
            return new ShopSnapshot(List.of(), OptionalInt.empty(), List.of());
        }
    }
}
