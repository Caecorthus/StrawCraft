package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunAmmoFactionTagsTest {
    @Test
    void resolvesExactlyOneDefaultFactionForWatheRoles() {
        assertEquals(GunAmmoFaction.POLICE, GunAmmoFactionTags.resolve(role(WatheRoleIds.VIGILANTE, true, false)).orElseThrow());
        assertEquals(GunAmmoFaction.KILLER, GunAmmoFactionTags.resolve(role(Identifier.of("wathe", "killer"), false, true)).orElseThrow());
        assertEquals(GunAmmoFaction.CIVILIAN, GunAmmoFactionTags.resolve(role(Identifier.of("wathe", "civilian"), true, false)).orElseThrow());
        Role customInnocent = new Role(Identifier.of("strawcraft", "veteran_fixture"), 0xFFFFFF, true, false, Role.MoodType.REAL, 200, false);
        assertEquals(GunAmmoFaction.CIVILIAN, GunAmmoFactionTags.resolve(customInnocent).orElseThrow());
    }

    @Test
    void refusesRolesThatMatchMultipleExplicitFactionTags() {
        GunAmmoFactionTags tags = GunAmmoFactionTags.empty()
                .withPoliceRole(Identifier.of("wathe", "civilian"))
                .withCivilianRole(Identifier.of("wathe", "civilian"));

        assertTrue(tags.resolveRole(role(Identifier.of("wathe", "civilian"), true, false)).isEmpty());
    }

    @Test
    void discoveryCivilianNeverReceivesAmmoEvenWhenExplicitlyTagged() {
        GunAmmoFactionTags tags = GunAmmoFactionTags.empty()
                .withPoliceRole(WatheRoleIds.DISCOVERY_CIVILIAN);

        assertTrue(tags.resolveRole(role(WatheRoleIds.DISCOVERY_CIVILIAN, true, false)).isEmpty());
    }

    private static Role role(Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
