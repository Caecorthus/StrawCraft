package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.caecorthus.strawcraft.api.StrawShopEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PoisonerShopLoadout {
    static final int POISON_PRICE = 50;

    private PoisonerShopLoadout() {
    }

    public static void registerPurchasePriceHandler() {
        StrawShopEvents.BEFORE_PURCHASE.register(PoisonerShopLoadout::applyPoisonerPrice);
    }

    static PlayerShopCatalog.Presentation presentation(List<ShopEntry> materializedEntries) {
        List<PlayerShopCatalog.VisibleEntry> visibleEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (isOfficialPoisonEntry(entry)) {
                visibleEntries.add(new PlayerShopCatalog.VisibleEntry(index, visibleEntry(entry)));
            }
        }
        return new PlayerShopCatalog.Presentation(visibleEntries);
    }

    static int priceForRole(@Nullable Role role, ShopEntry entry, int fallbackPrice) {
        return StrawRoleMeaning.usesPoisonerShop(role) && isOfficialPoisonEntry(entry)
                ? POISON_PRICE
                : fallbackPrice;
    }

    static boolean isOfficialPoisonEntry(ShopEntry entry) {
        return matchesOfficialPoison(entry, "poison_vial", stack -> stack.isOf(WatheItems.POISON_VIAL))
                || matchesOfficialPoison(entry, "scorpion", stack -> stack.isOf(WatheItems.SCORPION));
    }

    private static ShopEntry visibleEntry(ShopEntry entry) {
        Optional<StrawShopEntry> metadata = StrawShopEntry.metadata(entry);
        // Poisoner pays Spark's poison price without changing Wathe's global index order.
        // Poisoner 使用 Spark 的毒药价格，但不改变 Wathe 全局商店的索引顺序。
        // Poisoner 使用 Spark 毒物价格，但不改变 Wathe 全局 index 顺序。
        return new StrawShopEntry(
                StrawShopEntry.idFor(entry),
                StrawShopEntry.displayStackFor(entry),
                StrawShopEntry.actualStackFor(entry),
                POISON_PRICE,
                entry.type(),
                metadata.map(StrawShopEntry::cooldownTicks).orElse(0),
                metadata.map(StrawShopEntry::initialCooldownTicks).orElse(0),
                metadata.map(StrawShopEntry::maxStock).orElse(-1),
                entry
        );
    }

    private static boolean matchesOfficialPoison(ShopEntry entry, String path, PoisonStackPredicate stackPredicate) {
        String id = StrawShopEntry.idFor(entry);
        if (path.equals(id) || id.endsWith(":" + path)) {
            return true;
        }
        ItemStack stack = entry.stack();
        return stack != null && !stack.isEmpty() && stackPredicate.test(stack);
    }

    private static void applyPoisonerPrice(@Nullable PlayerEntity player, StrawShopEvents.PurchaseContext context) {
        if (player == null) {
            return;
        }
        try {
            Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
            context.setPrice(priceForRole(role, context.entry(), context.price()));
        } catch (RuntimeException ignored) {
            // Wathe role state may be unavailable while a round is settling; keep the current price.
            // 回合状态同步时 Wathe 角色可能暂不可用；此时保留当前价格。
        }
    }

    @FunctionalInterface
    private interface PoisonStackPredicate {
        boolean test(ItemStack stack);
    }
}
