package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleSemanticsTest {
    @Test
    void resolvesAmmoFactionFromNormalizedRoleKind() {
        assertEquals(Optional.of(GunAmmoFaction.KILLER),
                RoleSemantics.ammoFactionFor(StrawRoleMeaning.RoleKind.KILLER));
        assertEquals(Optional.of(GunAmmoFaction.POLICE),
                RoleSemantics.ammoFactionFor(StrawRoleMeaning.RoleKind.DETECTIVE));
        assertEquals(Optional.of(GunAmmoFaction.CIVILIAN),
                RoleSemantics.ammoFactionFor(StrawRoleMeaning.RoleKind.BYSTANDER));
    }

    @Test
    void leavesUnknownRoleKindWithoutAmmoFaction() {
        assertTrue(RoleSemantics.ammoFactionFor(StrawRoleMeaning.RoleKind.UNKNOWN).isEmpty());
    }
}
