package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VigilanteLoadoutTest {
    @Test
    void vigilanteLoadoutUsesTaczRhino357() {
        assertTrue(VigilanteLoadout.shouldReplaceAssignedRole(WatheRoles.VIGILANTE));
        assertEquals(TaczGunProfiles.RHINO357, VigilanteLoadout.VIGILANTE_GUN);

        NbtCompound customData = VigilanteLoadout.createRhino357CustomData().copyNbt();
        assertEquals("tacz:rhino357", customData.getString("GunId"));
    }

    @Test
    void killerAndVeteranLoadoutsAreNotChanged() {
        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(WatheRoles.KILLER));
        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(WatheRoles.VETERAN));
    }

    @Test
    void roleAssignedAdapterCleansInventoryForEveryRoleAndOnlyGrantsVigilanteGunToVigilantes() {
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, true),
                RoleAssignedLoadouts.planAssignedLoadout(WatheRoles.VIGILANTE, false));
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, false),
                RoleAssignedLoadouts.planAssignedLoadout(WatheRoles.KILLER, false));
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(false, false),
                RoleAssignedLoadouts.planAssignedLoadout(WatheRoles.VIGILANTE, true));
    }
}
