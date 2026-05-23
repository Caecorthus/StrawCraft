package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class KillerShopLoadout {
    public static final TaczGunProfile KILLER_GUN = TaczGunProfiles.P320;

    private static final String WATHE_REVOLVER_SHOP_ID = "revolver";
    private static final String KILLER_GUN_SHOP_ID = "p320";

    private KillerShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(KillerShopLoadout::replaceDisabledWatheGuns);
        WatheOfficialBridge.rewriteGlobalShopEntries();
    }

    static void replaceDisabledWatheGuns(StrawShopEvents.ShopContext context) {
        List<ShopEntry> rewrittenEntries = rewriteEntries(context.getEntries());
        context.replaceEntries(rewrittenEntries);
    }

    static List<ShopEntry> rewriteEntries(List<ShopEntry> originalEntries) {
        return rewriteEntries(
                originalEntries,
                KillerShopLoadout::replacementEntryForRevolver,
                KillerShopLoadout::containsDisabledWatheGun
        );
    }

    static List<ShopEntry> rewriteEntries(
            List<ShopEntry> originalEntries,
            Function<ShopEntry, Optional<ShopEntry>> revolverReplacementFactory,
            Predicate<ShopEntry> disabledGunPredicate
    ) {
        return rewriteEntries(originalEntries, KillerShopLoadout::isWatheRevolverShopEntry, revolverReplacementFactory, disabledGunPredicate);
    }

    static List<ShopEntry> rewriteEntries(
            List<ShopEntry> originalEntries,
            Predicate<ShopEntry> revolverPredicate,
            Function<ShopEntry, Optional<ShopEntry>> revolverReplacementFactory,
            Predicate<ShopEntry> disabledGunPredicate
    ) {
        List<ShopEntry> rewrittenEntries = new ArrayList<>(originalEntries.size());
        for (ShopEntry entry : originalEntries) {
            if (revolverPredicate.test(entry)) {
                // Replace in place so Wathe's visual order and StoreBuyPayload(index)
                // contract remain stable while the actual gun becomes TACZ-backed.
                // 原位替换可以保持 Wathe 的显示顺序和购买编号契约稳定，
                // 同时把实际发放的枪换成 TACZ 版本。
                revolverReplacementFactory.apply(entry).ifPresent(rewrittenEntries::add);
                continue;
            }
            if (!disabledGunPredicate.test(entry)) {
                rewrittenEntries.add(StrawShopEntry.decorate(entry));
            }
        }
        return List.copyOf(rewrittenEntries);
    }

    static ItemStack createP320Stack() {
        return TaczGunStacks.createGunStack(KILLER_GUN);
    }

    static NbtComponent createP320CustomData() {
        return TaczGunStacks.createGunCustomData(KILLER_GUN);
    }

    private static Optional<ShopEntry> replacementEntryForRevolver(ShopEntry original) {
        ItemStack p320 = createP320Stack();
        if (p320.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(replacementEntry(original, p320));
    }

    private static ShopEntry replacementEntry(ShopEntry original, ItemStack replacementStack) {
        ReplacementSettings settings = replacementSettingsFor(original);
        return new StrawShopEntry(
                settings.id(),
                replacementStack.copy(),
                replacementStack.copy(),
                settings.price(),
                settings.type(),
                settings.cooldownTicks(),
                settings.initialCooldownTicks(),
                settings.maxStock()
        );
    }

    static ReplacementSettings replacementSettingsFor(ShopEntry original) {
        Optional<StrawShopEntry> strawEntry = StrawShopEntry.metadata(original);
        return new ReplacementSettings(
                KILLER_GUN_SHOP_ID,
                original.price(),
                original.type(),
                strawEntry.map(StrawShopEntry::cooldownTicks).orElse(0),
                strawEntry.map(StrawShopEntry::initialCooldownTicks).orElse(0),
                strawEntry.map(StrawShopEntry::maxStock).orElse(-1)
        );
    }

    private static boolean isWatheRevolverShopEntry(ShopEntry entry) {
        return matchesStack(entry.stack(), stack -> stack.isOf(WatheItems.REVOLVER));
    }

    private static boolean containsDisabledWatheGun(ShopEntry entry) {
        return containsStack(entry, WeaponBalance::isDisabledWatheGun);
    }

    private static boolean containsStack(ShopEntry entry, Predicate<ItemStack> predicate) {
        return matchesStack(entry.stack(), predicate);
    }

    private static boolean matchesStack(ItemStack stack, Predicate<ItemStack> predicate) {
        return stack != null && !stack.isEmpty() && predicate.test(stack);
    }

    record ReplacementSettings(
            String id,
            int price,
            ShopEntry.Type type,
            int cooldownTicks,
            int initialCooldownTicks,
            int maxStock
    ) {
        boolean hasStockLimit() {
            return maxStock >= 0;
        }

        boolean hasCooldown() {
            return cooldownTicks > 0;
        }

        boolean hasInitialCooldown() {
            return initialCooldownTicks > 0;
        }
    }
}
