package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class KillerShopCatalog {
    public static final SupportedTaczGun KILLER_SUPPORTED_GUN = SupportedTaczGuns.P320;
    public static final TaczGunProfile KILLER_GUN = KILLER_SUPPORTED_GUN.profile();

    private final List<CatalogEntry> entries;

    private KillerShopCatalog(List<CatalogEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    static KillerShopCatalog materialize(List<ShopEntry> officialEntries) {
        return materialize(
                officialEntries,
                KillerShopCatalog::isWatheRevolverShopEntry,
                KillerShopCatalog::replacementEntryForRevolver,
                KillerShopCatalog::containsDisabledWatheGun
        );
    }

    static KillerShopCatalog materialize(
            List<ShopEntry> officialEntries,
            Function<ShopEntry, Optional<ShopEntry>> revolverReplacementFactory,
            Predicate<ShopEntry> disabledGunPredicate
    ) {
        return materialize(officialEntries, KillerShopCatalog::isWatheRevolverShopEntry, revolverReplacementFactory, disabledGunPredicate);
    }

    static KillerShopCatalog materialize(
            List<ShopEntry> officialEntries,
            Predicate<ShopEntry> revolverPredicate,
            Function<ShopEntry, Optional<ShopEntry>> revolverReplacementFactory,
            Predicate<ShopEntry> disabledGunPredicate
    ) {
        List<CatalogEntry> materializedEntries = new ArrayList<>(officialEntries.size());
        for (int originalIndex = 0; originalIndex < officialEntries.size(); originalIndex++) {
            ShopEntry entry = officialEntries.get(originalIndex);
            if (revolverPredicate.test(entry)) {
                Optional<ShopEntry> replacement = revolverReplacementFactory.apply(entry);
                if (replacement.isPresent()) {
                    materializedEntries.add(new CatalogEntry(originalIndex, replacement.orElseThrow()));
                }
                continue;
            }
            if (!disabledGunPredicate.test(entry)) {
                materializedEntries.add(new CatalogEntry(originalIndex, StrawShopEntry.decorate(entry)));
            }
        }
        return new KillerShopCatalog(materializedEntries);
    }

    List<ShopEntry> entries() {
        return entries.stream().map(CatalogEntry::entry).toList();
    }

    int originalPurchaseIndexAt(int catalogIndex) {
        return entries.get(catalogIndex).originalPurchaseIndex();
    }

    Optional<ShopEntry> entryForOriginalPurchaseIndex(int originalPurchaseIndex) {
        return entries.stream()
                .filter(entry -> entry.originalPurchaseIndex() == originalPurchaseIndex)
                .map(CatalogEntry::entry)
                .findFirst();
    }

    static ItemStack createP320Stack() {
        return KILLER_SUPPORTED_GUN.createGunStack();
    }

    static NbtComponent createP320CustomData() {
        return KILLER_SUPPORTED_GUN.createGunCustomData();
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
                KILLER_SUPPORTED_GUN.catalogId(),
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
        return matchesStack(entry.stack(), WeaponBalance::isDisabledWatheGun);
    }

    private static boolean matchesStack(ItemStack stack, Predicate<ItemStack> predicate) {
        return stack != null && !stack.isEmpty() && predicate.test(stack);
    }

    private record CatalogEntry(int originalPurchaseIndex, ShopEntry entry) {
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
