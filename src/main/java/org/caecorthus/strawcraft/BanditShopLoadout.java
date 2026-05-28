package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BanditShopLoadout {
    static final String THROWING_AXE_ENTRY_ID = "throwing_axe";
    static final int BANDIT_GUN_PRICE = 150;
    static final int THROWING_AXE_PRICE = 200;

    private BanditShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(BanditShopLoadout::addThrowingAxeEntry);
    }

    public static void registerPurchasePriceHandler() {
        StrawShopEvents.BEFORE_PURCHASE.register(BanditShopLoadout::applyBanditPrice);
    }

    static void addThrowingAxeEntry(StrawShopEvents.ShopContext ctx) {
        ctx.replaceEntries(withThrowingAxeEntry(ctx.getEntries()));
    }

    static List<ShopEntry> withThrowingAxeEntry(List<? extends ShopEntry> entries) {
        return withThrowingAxeEntry(entries, BanditShopLoadout::throwingAxeStack);
    }

    static List<ShopEntry> withThrowingAxeEntry(List<? extends ShopEntry> entries, Supplier<ItemStack> stackFactory) {
        if (entries.stream().anyMatch(BanditShopLoadout::isThrowingAxeEntry)) {
            return List.copyOf(entries);
        }

        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        boolean added = false;
        for (ShopEntry entry : entries) {
            nextEntries.add(entry);
            if (!added && isGunEntry(entry)) {
                nextEntries.add(throwingAxeEntry(stackFactory));
                added = true;
            }
        }
        if (!added) {
            nextEntries.add(throwingAxeEntry(stackFactory));
        }
        return List.copyOf(nextEntries);
    }

    static PlayerShopCatalog.Presentation presentation(List<ShopEntry> materializedEntries) {
        List<PlayerShopCatalog.VisibleEntry> visibleEntries = new ArrayList<>();
        for (int index = 0; index < materializedEntries.size(); index++) {
            ShopEntry entry = materializedEntries.get(index);
            if (isGunEntry(entry)) {
                visibleEntries.add(new PlayerShopCatalog.VisibleEntry(index, visibleGunEntry(entry)));
            } else if (!isBanditDeniedEntry(entry)) {
                visibleEntries.add(new PlayerShopCatalog.VisibleEntry(index, entry));
            }
        }
        return new PlayerShopCatalog.Presentation(visibleEntries);
    }

    static boolean isThrowingAxeEntry(ShopEntry entry) {
        return THROWING_AXE_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    static int priceForRole(@Nullable Role role, ShopEntry entry, int fallbackPrice) {
        return StrawRoleMeaning.usesBanditShop(role) && isGunEntry(entry)
                ? BANDIT_GUN_PRICE
                : fallbackPrice;
    }

    static boolean isBanditDeniedEntry(ShopEntry entry) {
        return isThrowingAxeEntry(entry) ? false
                : ReporterShopLoadout.isReporterNoteEntry(entry)
                || BomberTimedBombRuntime.isTimedBombEntry(entry)
                || TimekeeperShopLoadout.isTimeSubtractionEntry(entry)
                || BartenderDefenseVialShopLoadout.isDefenseVialEntry(entry)
                || WaiterShopLoadout.isWaiterServiceEntry(entry)
                || SilencerShopLoadout.isSilentPsychoEntry(entry)
                || PoisonerShopLoadout.isPoisonNeedleEntry(entry)
                || ScavengerShopLoadout.isResetKnifeCooldownEntry(entry)
                || matches(entry, "revolver", stack -> stack.isOf(WatheItems.REVOLVER))
                || matches(entry, "grenade", stack -> stack.isOf(WatheItems.GRENADE))
                || matches(entry, "poison_vial", stack -> stack.isOf(WatheItems.POISON_VIAL))
                || matches(entry, "scorpion", stack -> stack.isOf(WatheItems.SCORPION))
                || matches(entry, "psycho_mode", stack -> stack.isOf(WatheItems.PSYCHO_MODE));
    }

    private static ShopEntry visibleGunEntry(ShopEntry entry) {
        Optional<StrawShopEntry> metadata = StrawShopEntry.metadata(entry);
        return new StrawShopEntry(
                StrawShopEntry.idFor(entry),
                StrawShopEntry.displayStackFor(entry),
                StrawShopEntry.actualStackFor(entry),
                BANDIT_GUN_PRICE,
                entry.type(),
                metadata.map(StrawShopEntry::cooldownTicks).orElse(0),
                metadata.map(StrawShopEntry::initialCooldownTicks).orElse(0),
                metadata.map(StrawShopEntry::maxStock).orElse(-1),
                entry
        );
    }

    private static boolean isGunEntry(ShopEntry entry) {
        return matches(entry, KillerShopCatalog.KILLER_SUPPORTED_GUN.catalogId(), stack -> false);
    }

    private static boolean matches(ShopEntry entry, String path, Predicate<ItemStack> stackPredicate) {
        String id = StrawShopEntry.idFor(entry);
        if (path.equals(id) || id.endsWith(":" + path)) {
            return true;
        }
        ItemStack stack = entry.stack();
        return stack != null && !stack.isEmpty() && stackPredicate.test(stack);
    }

    private static ItemStack throwingAxeStack() {
        ItemStack stack = StrawCraftItems.THROWING_AXE.getDefaultStack();
        stack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.strawcraft." + THROWING_AXE_ENTRY_ID)
        );
        return stack;
    }

    private static ShopEntry throwingAxeEntry(Supplier<ItemStack> stackFactory) {
        ItemStack stack = stackFactory.get();
        ShopEntry delivery = new ShopEntry(stack, THROWING_AXE_PRICE, ShopEntry.Type.WEAPON);
        return new StrawShopEntry(
                THROWING_AXE_ENTRY_ID,
                stack,
                stack,
                THROWING_AXE_PRICE,
                ShopEntry.Type.WEAPON,
                0,
                0,
                -1,
                delivery
        );
    }

    private static void applyBanditPrice(@Nullable PlayerEntity player, StrawShopEvents.PurchaseContext ctx) {
        if (player == null) {
            return;
        }
        try {
            Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
            ctx.setPrice(priceForRole(role, ctx.entry(), ctx.price()));
        } catch (RuntimeException ignored) {
            // Wathe role state may be unavailable while a round is settling; keep the current price.
            // Wathe 职业状态在回合切换时可能暂不可用；此时保留当前价格。
        }
    }
}
