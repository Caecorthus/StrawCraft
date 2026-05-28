package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void explicitPoliceAmmoRoleDoesNotBecomeVigilanteLoadoutRole() {
        Role customPoliceAmmoRole = role(Identifier.of("strawcraft", "police_ammo_fixture"), true, false);
        GunAmmoFactionTags tags = GunAmmoFactionTags.empty()
                .withPoliceRole(customPoliceAmmoRole.identifier());

        assertEquals(GunAmmoFaction.POLICE, tags.resolveRole(customPoliceAmmoRole).orElseThrow());
        assertFalse(VigilanteLoadout.shouldReplaceAssignedRole(customPoliceAmmoRole));
    }

    @Test
    void roleAssignedAdapterCleansInventoryForEveryRoleAndOnlyGrantsVigilanteGunToVigilantes() {
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, true, false, false, List.of(), List.of()),
                RoleAssignedLoadouts.planAssignedLoadout(role(WatheRoleIds.VIGILANTE, true, false), false));
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, false, false, false, List.of(), List.of()),
                RoleAssignedLoadouts.planAssignedLoadout(role(Identifier.of("wathe", "killer"), false, true), false));
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(false, false, false, false, List.of(), List.of()),
                RoleAssignedLoadouts.planAssignedLoadout(role(WatheRoleIds.VIGILANTE, true, false), true));
    }

    @Test
    void undercoverWalkieTalkieIsDeferredByNoellesAssignedLoadoutSlice() {
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(
                        true,
                        false,
                        false,
                        false,
                        List.of(),
                        List.of(new NoellesAssignedLoadouts.UnsupportedItemGrant(
                                Identifier.of("wathe", "walkie_talkie"),
                                1,
                                "XruiDD Undercover walkie-talkie depends on Spark-ver Wathe; official Wathe has no wathe:walkie_talkie item"
                        ))
                ),
                RoleAssignedLoadouts.planAssignedLoadout(role(Identifier.of("strawcraft", "undercover"), true, false), false));
    }

    @Test
    void otherNoellesRolesDoNotReceiveTheUndercoverWalkieTalkie() {
        RoleAssignedLoadouts.AssignmentPlan conductorPlan =
                RoleAssignedLoadouts.planAssignedLoadout(role(Identifier.of("strawcraft", "conductor"), true, false), false);

        assertEquals(List.of(new NoellesAssignedLoadouts.ItemGrant(StrawCraft.id("master_key"), 1)),
                conductorPlan.assignmentItemGrants());
        assertTrue(conductorPlan.unsupportedItemGrants().isEmpty());
        assertEquals(new RoleAssignedLoadouts.AssignmentPlan(true, false, false, false, List.of(), List.of()),
                RoleAssignedLoadouts.planAssignedLoadout(role(Identifier.of("strawcraft", "scavenger"), false, true), false));
    }

    @Test
    void professorAssignmentGrantsInitialIronManProtectionInsteadOfAFakeVial() {
        RoleAssignedLoadouts.AssignmentPlan plan =
                RoleAssignedLoadouts.planAssignedLoadout(role(Identifier.of("strawcraft", "professor"), true, false), false);

        assertTrue(plan.grantProfessorIronManProtection());
        assertTrue(plan.assignmentItemGrants().isEmpty());
        assertTrue(plan.unsupportedItemGrants().isEmpty());
    }

    private static Role role(Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
