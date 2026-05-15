package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

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
        BuildShopEntries.EVENT.register((player, context) -> replaceDisabledWatheGuns(context));
    }

    static void replaceDisabledWatheGuns(BuildShopEntries.ShopContext context) {
        List<ShopEntry> rewrittenEntries = rewriteEntries(context.getEntries());
        context.clearEntries();
        rewrittenEntries.forEach(context::addEntry);
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
        List<ShopEntry> rewrittenEntries = new ArrayList<>(originalEntries.size());
        for (ShopEntry entry : originalEntries) {
            if (isWatheRevolverShopEntry(entry)) {
                revolverReplacementFactory.apply(entry).ifPresent(rewrittenEntries::add);
                continue;
            }
            if (!disabledGunPredicate.test(entry)) {
                rewrittenEntries.add(entry);
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
        ShopEntry.Builder builder = new ShopEntry.Builder(
                settings.id(),
                replacementStack.copy(),
                settings.price(),
                settings.type()
        ).actualStack(replacementStack.copy());

        if (settings.hasStockLimit()) {
            builder.stock(settings.maxStock());
        }
        if (settings.hasCooldown()) {
            builder.cooldown(settings.cooldownTicks());
        }
        if (settings.hasInitialCooldown()) {
            builder.initialCooldown(settings.initialCooldownTicks());
        }

        return builder.build();
    }

    static ReplacementSettings replacementSettingsFor(ShopEntry original) {
        return new ReplacementSettings(
                KILLER_GUN_SHOP_ID,
                original.price(),
                original.type(),
                original.cooldownTicks(),
                original.initialCooldownTicks(),
                original.maxStock()
        );
    }

    private static boolean isWatheRevolverShopEntry(ShopEntry entry) {
        return WATHE_REVOLVER_SHOP_ID.equals(entry.id());
    }

    private static boolean containsDisabledWatheGun(ShopEntry entry) {
        return containsStack(entry, WeaponBalance::isDisabledWatheGun);
    }

    private static boolean containsStack(ShopEntry entry, Predicate<ItemStack> predicate) {
        return matchesStack(entry.displayStack(), predicate) || matchesStack(entry.getActualStack(), predicate);
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
