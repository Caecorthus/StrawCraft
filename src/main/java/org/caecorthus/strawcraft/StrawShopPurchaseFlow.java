package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.caecorthus.strawcraft.api.StrawShopEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StrawShopPurchaseFlow {
    private StrawShopPurchaseFlow() {
    }

    public static PurchaseStatus tryBuy(PlayerEntity player, int index, Account account) {
        return tryBuy(
                player,
                index,
                GameConstants.SHOP_ENTRIES,
                account,
                EntryDelivery.DEFAULT,
                ShopStateAccess.forPlayer(player),
                ShopAccess.WATHE_KILLER_FEATURES,
                ItemCooldowns.WATHE_ITEM_COOLDOWNS,
                StrawShopPurchaseFlow::playBuySound
        );
    }

    public static PurchaseStatus tryBuy(
            @Nullable PlayerEntity player,
            int index,
            List<ShopEntry> baseEntries,
            Account account,
            EntryDelivery delivery,
            ShopStateAccess shopState,
            ShopAccess shopAccess,
            SuccessSound successSound
    ) {
        return tryBuy(player, index, baseEntries, account, delivery, shopState, shopAccess, ItemCooldowns.NONE, successSound);
    }

    public static PurchaseStatus tryBuy(
            @Nullable PlayerEntity player,
            int index,
            List<ShopEntry> baseEntries,
            Account account,
            EntryDelivery delivery,
            ShopStateAccess shopState,
            ShopAccess shopAccess,
            ItemCooldowns itemCooldowns,
            SuccessSound successSound
    ) {
        if (player != null && !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return PurchaseStatus.PLAYER_UNAVAILABLE;
        }

        List<ShopEntry> entries = List.copyOf(baseEntries);
        if (index < 0 || index >= entries.size()) {
            return PurchaseStatus.INVALID_INDEX;
        }

        ShopEntry entry = entries.get(index);
        @Nullable StrawShopEntry strawEntry = StrawShopEntry.metadata(entry).orElse(null);
        if (strawEntry != null && strawEntry.requiresShopAccess() && !shopAccess.canUseShop(player)) {
            return PurchaseStatus.SHOP_ACCESS_DENIED;
        }
        if (itemCooldowns.isCoolingDown(player, entry)) {
            return PurchaseStatus.ITEM_COOLDOWN;
        }

        long now = player == null ? 0L : player.getWorld().getTime();
        if (strawEntry != null) {
            shopState.ensureEntry(strawEntry, now);
            if (!shopState.canPurchase(strawEntry, now)) {
                return PurchaseStatus.STATE_BLOCKED;
            }
        }

        StrawShopEvents.PurchaseContext context = StrawShopEvents.beforePurchase(player, entry, index);
        if (!context.allowed()) {
            if (player != null && context.denyReason() != null) {
                player.sendMessage(context.denyReason(), true);
            }
            return PurchaseStatus.DENIED;
        }

        int actualPrice = context.price();
        if (account.balance() < actualPrice) {
            return PurchaseStatus.INSUFFICIENT_BALANCE;
        }
        if (!delivery.buy(entry, player)) {
            return PurchaseStatus.DELIVERY_FAILED;
        }

        account.setBalance(account.balance() - actualPrice);
        if (strawEntry != null) {
            shopState.recordPurchase(strawEntry, now);
        }
        account.sync();
        successSound.play(player);
        StrawShopEvents.afterPurchase(player, context.receipt());
        return PurchaseStatus.SUCCESS;
    }

    private static void playBuySound(@Nullable PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(WatheSounds.UI_SHOP_BUY),
                SoundCategory.MASTER,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                1.0F,
                0.9F + serverPlayer.getRandom().nextFloat() * 0.2F,
                serverPlayer.getRandom().nextLong()
        ));
    }

    public interface Account {
        int balance();

        void setBalance(int balance);

        void sync();
    }

    public interface EntryDelivery {
        EntryDelivery DEFAULT = (entry, player) -> player != null && entry.onBuy(player);

        boolean buy(ShopEntry entry, @Nullable PlayerEntity player);
    }

    public interface ShopStateAccess {
        ShopStateAccess NOOP = new ShopStateAccess() {
            @Override
            public void ensureEntry(StrawShopEntry entry, long now) {
            }

            @Override
            public boolean canPurchase(StrawShopEntry entry, long now) {
                return true;
            }

            @Override
            public void recordPurchase(StrawShopEntry entry, long now) {
            }
        };

        static ShopStateAccess forPlayer(@Nullable PlayerEntity player) {
            if (player == null) {
                return NOOP;
            }
            StrawPlayerShopComponent component = StrawPlayerShopComponent.KEY.get(player);
            return new ShopStateAccess() {
                @Override
                public void ensureEntry(StrawShopEntry entry, long now) {
                    component.ensureEntry(entry, now);
                }

                @Override
                public boolean canPurchase(StrawShopEntry entry, long now) {
                    return component.canPurchase(entry, now);
                }

                @Override
                public void recordPurchase(StrawShopEntry entry, long now) {
                    component.recordPurchase(entry, now);
                }
            };
        }

        void ensureEntry(StrawShopEntry entry, long now);

        boolean canPurchase(StrawShopEntry entry, long now);

        void recordPurchase(StrawShopEntry entry, long now);
    }

    public interface ShopAccess {
        ShopAccess WATHE_KILLER_FEATURES = player -> player != null
                && GameWorldComponent.KEY.get(player.getWorld()).canUseKillerFeatures(player);

        boolean canUseShop(@Nullable PlayerEntity player);
    }

    public interface ItemCooldowns {
        ItemCooldowns NONE = (player, entry) -> false;
        ItemCooldowns WATHE_ITEM_COOLDOWNS = (player, entry) -> player != null
                && entry.stack() != null
                && !entry.stack().isEmpty()
                && player.getItemCooldownManager().isCoolingDown(entry.stack().getItem());

        boolean isCoolingDown(@Nullable PlayerEntity player, ShopEntry entry);
    }

    public interface SuccessSound {
        void play(@Nullable PlayerEntity player);
    }

    public enum PurchaseStatus {
        SUCCESS,
        PLAYER_UNAVAILABLE,
        INVALID_INDEX,
        SHOP_ACCESS_DENIED,
        ITEM_COOLDOWN,
        INSUFFICIENT_BALANCE,
        STATE_BLOCKED,
        DENIED,
        DELIVERY_FAILED
    }
}
