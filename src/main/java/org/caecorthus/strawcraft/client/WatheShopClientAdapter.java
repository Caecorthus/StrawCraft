package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.StoreBuyPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.caecorthus.strawcraft.api.StrawShopEvents;

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
        return canAccessShop(player) && !entriesFor(player).isEmpty();
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
        // Wathe 会在服务端重新校验编号，然后才完成购买。
        ClientPlayNetworking.send(new StoreBuyPayload(index));
    }

    public static ShopSnapshot snapshotFrom(List<ShopEntry> entries, ShopState shopState) {
        List<ShopEntryViewState> entryStates = new ArrayList<>(entries.size());
        List<EntryKey> entryKeys = new ArrayList<>(entries.size());
        for (ShopEntry entry : entries) {
            entryStates.add(viewStateFor(entry, shopState));
            // Entry keys capture identity only; price/cooldown/stock can update without
            // rebuilding buttons and disturbing Wathe's server-side StoreBuyPayload(index).
            // 条目键只记录身份；价格、冷却和库存可以更新，
            // 不需要重建按钮，也不会扰动 Wathe 服务端的购买编号。
            entryKeys.add(EntryKey.from(entry));
        }
        OptionalInt balance = shopState == null ? OptionalInt.empty() : shopState.balance();
        return new ShopSnapshot(List.copyOf(entries), balance, List.copyOf(entryStates), List.copyOf(entryKeys));
    }

    private static ShopEntryViewState viewStateFor(ShopEntry entry, ShopState shopState) {
        if (shopState == null) {
            return ShopEntryViewState.withoutShop(entry.price());
        }
        return ShopEntryViewState.fromSnapshot(new ShopEntryViewState.Snapshot(
                entry.price(),
                false,
                0,
                -1,
                -1,
                true
        ));
    }

    private static ClientPlayerEntity player(MinecraftClient client) {
        return client == null ? null : client.player;
    }

    private static List<ShopEntry> entriesFor(ClientPlayerEntity player) {
        if (!canAccessShop(player)) {
            return List.of();
        }
        try {
            return StrawShopEvents.modifyEntries(player, GameConstants.SHOP_ENTRIES);
        } catch (RuntimeException ignored) {
            // Wathe can throw while client-side role/shop components are still catching up.
            // 客户端角色或商店组件还在同步时，Wathe 可能会暂时抛错。
            return List.of();
        }
    }

    private static boolean canAccessShop(ClientPlayerEntity player) {
        try {
            // Match official Wathe's shop visibility: only killer-feature roles see the store.
            // 对齐官方 Wathe 的商店显示条件：只有能使用杀手功能的职业能看到商店。
            return GameWorldComponent.KEY.get(player.getWorld()).canUseKillerFeatures(player);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public interface ShopState {
        OptionalInt balance();
    }

    private record PlayerShopState(PlayerShopComponent shop) implements ShopState {
        @Override
        public OptionalInt balance() {
            return OptionalInt.of(shop.balance);
        }
    }

    public record ShopSnapshot(
            List<ShopEntry> entries,
            OptionalInt balance,
            List<ShopEntryViewState> entryStates,
            List<EntryKey> entryKeys
    ) {
        public static ShopSnapshot empty() {
            return new ShopSnapshot(List.of(), OptionalInt.empty(), List.of(), List.of());
        }
    }

    public record EntryKey(
            int price,
            ShopEntry.Type type,
            StackKey stack
    ) {
        private static EntryKey from(ShopEntry entry) {
            return new EntryKey(
                    entry.price(),
                    entry.type(),
                    StackKey.from(entry.stack())
            );
        }
    }

    public record StackKey(String itemId, int count, String name, String customData) {
        private static StackKey from(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return new StackKey("", 0, "", "");
            }
            NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            // TACZ uses custom data such as GunId to distinguish concrete guns on the same item.
            // TACZ 会用 GunId 这类自定义数据区分同一个物品上的具体枪械。
            return new StackKey(
                    Registries.ITEM.getId(stack.getItem()).toString(),
                    stack.getCount(),
                    stack.getName().getString(),
                    customData.copyNbt().toString()
            );
        }
    }
}
