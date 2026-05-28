package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.util.ShopEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoisonerShopLoadoutTest {
    @Test
    void poisonerPaysConfiguredPriceForOfficialPoisonEntriesOnly() {
        Role poisoner = role("poisoner", true, false);
        Role killer = role("killer", true, false);
        ShopEntry poison = entry("poison_vial", 75, ShopEntry.Type.POISON);
        ShopEntry scorpion = entry("scorpion", 75, ShopEntry.Type.POISON);
        ShopEntry psycho = entry("psycho_mode", 150, ShopEntry.Type.POISON);

        assertEquals(50, PoisonerShopLoadout.priceForRole(poisoner, poison, poison.price()));
        assertEquals(50, PoisonerShopLoadout.priceForRole(poisoner, scorpion, scorpion.price()));
        assertEquals(150, PoisonerShopLoadout.priceForRole(poisoner, psycho, psycho.price()));
        assertEquals(75, PoisonerShopLoadout.priceForRole(killer, poison, poison.price()));
    }

    private static StrawShopEntry entry(String id, int price, ShopEntry.Type type) {
        return new StrawShopEntry(id, null, null, price, type, 0, 0, -1);
    }

    private static Role role(String path, boolean killerTools, boolean innocent) {
        return new Role(StrawCraft.id(path), 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
