package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BanditShopLoadoutTest {
    @Test
    void addsThrowingAxeAfterExistingGunSlotOnlyOnce() {
        ShopEntry knife = entry("knife", 100);
        ShopEntry gun = entry("p320", 300);
        ShopEntry grenade = entry("grenade", 350);

        List<ShopEntry> entries = BanditShopLoadout.withThrowingAxeEntry(
                List.of(knife, gun, grenade),
                BanditShopLoadoutTest::throwingAxeStack
        );

        assertEquals(List.of(
                "knife",
                "p320",
                BanditShopLoadout.THROWING_AXE_ENTRY_ID,
                "grenade"
        ), entries.stream().map(StrawShopEntry::idFor).toList());
        assertEquals(BanditShopLoadout.THROWING_AXE_PRICE, entries.get(2).price());
        assertEquals(ShopEntry.Type.WEAPON, entries.get(2).type());

        List<ShopEntry> unchanged = BanditShopLoadout.withThrowingAxeEntry(entries, BanditShopLoadoutTest::throwingAxeStack);
        assertEquals(entries, unchanged);
    }

    @Test
    void appendsThrowingAxeWhenGunSlotIsUnavailable() {
        ShopEntry knife = entry("knife", 100);

        List<ShopEntry> entries = BanditShopLoadout.withThrowingAxeEntry(
                List.of(knife),
                BanditShopLoadoutTest::throwingAxeStack
        );

        assertEquals(List.of("knife", BanditShopLoadout.THROWING_AXE_ENTRY_ID), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
    }

    @Test
    void banditPresentationKeepsKnifeDiscountedGunAndAxeWithOriginalPurchaseIndices() {
        ShopEntry knife = entry("knife", 100);
        ShopEntry gun = entry("p320", 300);
        ShopEntry axe = entry(BanditShopLoadout.THROWING_AXE_ENTRY_ID, BanditShopLoadout.THROWING_AXE_PRICE);
        ShopEntry grenade = entry("grenade", 350);
        ShopEntry psycho = entry("psycho_mode", 150);
        ShopEntry lockpick = entry("lockpick", 50);

        PlayerShopCatalog.Presentation presentation = BanditShopLoadout.presentation(
                List.of(knife, gun, axe, grenade, psycho, lockpick)
        );

        assertEquals(List.of("knife", "p320", BanditShopLoadout.THROWING_AXE_ENTRY_ID, "lockpick"), presentation.entries().stream()
                .map(StrawShopEntry::idFor)
                .toList());
        assertSame(knife, presentation.entries().get(0));
        assertEquals(150, presentation.entries().get(1).price());
        assertEquals(BanditShopLoadout.THROWING_AXE_PRICE, presentation.entries().get(2).price());
        assertEquals(List.of(0, 1, 2, 5), presentation.visibleEntries().stream()
                .map(PlayerShopCatalog.VisibleEntry::wathePurchaseIndex)
                .toList());
    }

    @Test
    void banditGunPriceAppliesToServerPurchasePriceWithoutDiscountingOtherRoles() {
        ShopEntry gun = entry("p320", 300);

        assertEquals(150, BanditShopLoadout.priceForRole(role("bandit"), gun, gun.price()));
        assertEquals(300, BanditShopLoadout.priceForRole(role("killer"), gun, gun.price()));
        assertEquals(300, BanditShopLoadout.priceForRole(role("bandit"), entry("knife", 100), 300));
    }

    private static StrawShopEntry entry(String id, int price) {
        return new StrawShopEntry(id, null, null, price, ShopEntry.Type.WEAPON, 0, 0, -1);
    }

    private static net.minecraft.item.ItemStack throwingAxeStack() {
        return null;
    }

    private static Role role(String path) {
        return new Role(StrawCraft.id(path), 0xFFFFFF, false, true, Role.MoodType.REAL, 200, false);
    }
}
