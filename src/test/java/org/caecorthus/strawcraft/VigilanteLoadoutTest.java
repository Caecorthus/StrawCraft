package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VigilanteLoadoutTest {
    @Test
    void vigilanteLoadoutUsesTaczRhino357() {
        assertTrue(VigilanteLoadout.shouldReplaceAssignedRole(role(WatheRoleIds.VIGILANTE, true, false)));
        assertEquals(TaczGunProfiles.RHINO357, VigilanteLoadout.VIGILANTE_GUN);

        NbtCompound customData = VigilanteLoadout.createRhino357CustomData().copyNbt();
        assertEquals("tacz:rhino357", customData.getString("GunId"));
    }

    @Test
    void killerAndCustomInnocentLoadoutsAreNotChanged() {
        Role customInnocent = new Role(Identifier.of("strawcraft", "veteran_fixture"), 0xFFFFFF, true, false, Role.MoodType.REAL, 200, false);

        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(role(Identifier.of("wathe", "killer"), false, true)));
        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(customInnocent));
    }

    @Test
    void roleAssignedAdapterCleansInventoryForEveryRoleAndOnlyGrantsVigilanteGunToVigilantes() {
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, true),
                RoleAssignedLoadouts.planAssignedLoadout(role(WatheRoleIds.VIGILANTE, true, false), false));
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, false),
                RoleAssignedLoadouts.planAssignedLoadout(role(Identifier.of("wathe", "killer"), false, true), false));
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(false, false),
                RoleAssignedLoadouts.planAssignedLoadout(role(WatheRoleIds.VIGILANTE, true, false), true));
    }

    private static Role role(Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
