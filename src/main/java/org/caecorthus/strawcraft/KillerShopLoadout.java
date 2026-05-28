package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class KillerShopLoadout {
    public static final TaczGunProfile KILLER_GUN = KillerShopCatalog.KILLER_GUN;

    private KillerShopLoadout() {
    }

    public static void registerShopEntriesHandler() {
        StrawShopEvents.BUILD_ENTRIES.register(KillerShopLoadout::replaceDisabledWatheGuns);
        ScavengerShopLoadout.registerShopEntriesHandler();
    }

    static void replaceDisabledWatheGuns(StrawShopEvents.ShopContext context) {
        List<ShopEntry> rewrittenEntries = rewriteEntries(context.getEntries());
        context.replaceEntries(rewrittenEntries);
    }

    static List<ShopEntry> rewriteEntries(List<ShopEntry> originalEntries) {
        return KillerShopCatalog.materialize(originalEntries).entries();
    }

    static List<ShopEntry> rewriteEntries(
            List<ShopEntry> originalEntries,
            Function<ShopEntry, Optional<ShopEntry>> revolverReplacementFactory,
            Predicate<ShopEntry> disabledGunPredicate
    ) {
        return KillerShopCatalog.materialize(originalEntries, revolverReplacementFactory, disabledGunPredicate).entries();
    }

    static List<ShopEntry> rewriteEntries(
            List<ShopEntry> originalEntries,
            Predicate<ShopEntry> revolverPredicate,
            Function<ShopEntry, Optional<ShopEntry>> revolverReplacementFactory,
            Predicate<ShopEntry> disabledGunPredicate
    ) {
        return KillerShopCatalog.materialize(originalEntries, revolverPredicate, revolverReplacementFactory, disabledGunPredicate).entries();
    }

    static ItemStack createP320Stack() {
        return KillerShopCatalog.createP320Stack();
    }

    static NbtComponent createP320CustomData() {
        return KillerShopCatalog.createP320CustomData();
    }

    static KillerShopCatalog.ReplacementSettings replacementSettingsFor(ShopEntry original) {
        return KillerShopCatalog.replacementSettingsFor(original);
    }
}
