package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;
import java.util.function.Supplier;

public final class KillerShopLoadout {
    public static final TaczGunProfile KILLER_GUN = TaczGunProfiles.P320;

    private static final String KILLER_GUN_SHOP_ID = "p320";

    private KillerShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        BuildShopEntries.EVENT.register((player, context) -> replaceDisabledWatheGuns(context));
    }

    static void replaceDisabledWatheGuns(BuildShopEntries.ShopContext context) {
        replaceDisabledWatheGuns(
                context,
                KillerShopLoadout::createP320Stack,
                KillerShopLoadout::isWatheRevolverStack,
                WeaponBalance::isDisabledWatheGun
        );
    }

    static void replaceDisabledWatheGuns(
            BuildShopEntries.ShopContext context,
            Supplier<ItemStack> p320StackFactory,
            Predicate<ItemStack> revolverPredicate,
            Predicate<ItemStack> disabledGunPredicate
    ) {
        for (int index = 0; index < context.size(); index++) {
            ShopEntry entry = context.getEntry(index);
            if (containsStack(entry, revolverPredicate)) {
                ItemStack p320 = p320StackFactory.get();
                if (p320.isEmpty()) {
                    context.removeEntry(index--);
                    continue;
                }
                context.setEntry(index, replacementEntry(entry, p320));
            } else if (containsStack(entry, disabledGunPredicate)) {
                context.removeEntry(index--);
            }
        }
    }

    static ItemStack createP320Stack() {
        return TaczGunStacks.createGunStack(KILLER_GUN);
    }

    static NbtComponent createP320CustomData() {
        return TaczGunStacks.createGunCustomData(KILLER_GUN);
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

    private static boolean isWatheRevolverStack(ItemStack stack) {
        return stack.isOf(WatheItems.REVOLVER);
    }

    private static boolean containsStack(ShopEntry entry, Predicate<ItemStack> predicate) {
        return predicate.test(entry.displayStack()) || predicate.test(entry.getActualStack());
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
