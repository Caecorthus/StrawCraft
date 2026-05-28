package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BartenderDefenseVialPolicyTest {
    @Test
    void bartenderCanClearOnlyTheirOwnActivePoison() {
        assertEquals(BartenderDefenseVialPolicy.DefenseTarget.SELF, BartenderDefenseVialPolicy.selectDefenseTarget(
                input("bartender", true, true)
        ));
    }

    @Test
    void deadUnpoisonedOrWrongRoleCarrierIsDenied() {
        assertEquals(BartenderDefenseVialPolicy.DefenseTarget.NONE, BartenderDefenseVialPolicy.selectDefenseTarget(
                input("bartender", false, true)
        ));
        assertEquals(BartenderDefenseVialPolicy.DefenseTarget.NONE, BartenderDefenseVialPolicy.selectDefenseTarget(
                input("bartender", true, false)
        ));
        assertEquals(BartenderDefenseVialPolicy.DefenseTarget.NONE, BartenderDefenseVialPolicy.selectDefenseTarget(
                input("toxicologist", true, true)
        ));
        assertEquals(BartenderDefenseVialPolicy.DefenseTarget.NONE, BartenderDefenseVialPolicy.selectDefenseTarget(
                input("detective", true, true)
        ));
    }

    private static BartenderDefenseVialPolicy.Input input(String role, boolean carrierAlive, boolean selfPoisoned) {
        return new BartenderDefenseVialPolicy.Input(noellesRole(role), carrierAlive, selfPoisoned);
    }

    private static Role noellesRole(String path) {
        return NoellesRoleCatalog.find(StrawCraft.id(path)).orElseThrow().watheRole();
    }
}
