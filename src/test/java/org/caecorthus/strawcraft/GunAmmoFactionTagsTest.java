package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunAmmoFactionTagsTest {
    @Test
    void resolvesExactlyOneDefaultFactionForWatheRoles() {
        assertEquals(GunAmmoFaction.POLICE, GunAmmoFactionTags.resolve(WatheRoles.VIGILANTE).orElseThrow());
        assertEquals(GunAmmoFaction.KILLER, GunAmmoFactionTags.resolve(WatheRoles.KILLER).orElseThrow());
        assertEquals(GunAmmoFaction.CIVILIAN, GunAmmoFactionTags.resolve(WatheRoles.CIVILIAN).orElseThrow());
        assertEquals(GunAmmoFaction.CIVILIAN, GunAmmoFactionTags.resolve(WatheRoles.VETERAN).orElseThrow());
    }

    @Test
    void refusesRolesThatMatchMultipleExplicitFactionTags() {
        GunAmmoFactionTags tags = GunAmmoFactionTags.empty()
                .withPoliceRole(WatheRoles.CIVILIAN.identifier())
                .withCivilianRole(WatheRoles.CIVILIAN.identifier());

        assertTrue(tags.resolveRole(WatheRoles.CIVILIAN).isEmpty());
    }
}
