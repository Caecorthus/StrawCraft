package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawRoleEvents;

import java.util.List;

public final class RoleAssignedLoadouts {
    private RoleAssignedLoadouts() {
    }

    public static void register() {
        StrawRoleEvents.ROLE_ASSIGNED.register(RoleAssignedLoadouts::applyAssignedLoadout);
    }

    static AssignmentPlan planAssignedLoadout(Role role, boolean clientWorld) {
        if (clientWorld) {
            return AssignmentPlan.nothing();
        }
        NoellesAssignedLoadouts.LoadoutPlan noellesPlan = NoellesAssignedLoadouts.assignmentPlan(role);
        return new AssignmentPlan(
                true,
                VigilanteLoadout.shouldReplaceAssignedRole(role),
                StrawRoleMeaning.receivesProfessorIronManProtection(role),
                StrawRoleMeaning.receivesBodyguardProtection(role),
                noellesPlan.itemGrants(),
                noellesPlan.unsupportedItemGrants()
        );
    }

    private static void applyAssignedLoadout(PlayerEntity player, Role role) {
        AssignmentPlan plan = planAssignedLoadout(role, player.getWorld().isClient());
        if (plan.removeDisabledWatheGuns()) {
            RoundInventoryCleanup.removeDisabledWatheGuns(player.getInventory());
        }
        if (plan.grantVigilanteGun()) {
            VigilanteLoadout.giveAssignedLoadout(player);
        }
        if (plan.grantProfessorIronManProtection()) {
            ProfessorIronManProtection.grant(NoellesRoleStateComponent.KEY.get(player), player.getWorld().getTime());
        }
        if (plan.grantBodyguardProtection()) {
            BodyguardProtectionPolicy.grant(NoellesRoleStateComponent.KEY.get(player), player.getWorld().getTime());
        }
        if (StrawRoleMeaning.matchesRoleId(role, VultureBodyFeastPolicy.VULTURE_ROLE)) {
            int totalPlayers = GameWorldComponent.KEY.get(player.getWorld()).getRoles().size();
            VultureBodyFeastPolicy.resetRoundState(NoellesRoleStateComponent.KEY.get(player), totalPlayers);
        }
        NoellesAssignedLoadouts.giveAssignedItems(player, plan.assignmentItemGrants());
        if (grantsItem(plan.assignmentItemGrants(), StrawCraftItems.ANTIDOTE_ID)) {
            ToxicologistAntidoteItem.applyInitialAssignmentCooldown(player);
        }
    }

    private static boolean grantsItem(List<NoellesAssignedLoadouts.ItemGrant> itemGrants, Identifier itemId) {
        return itemGrants.stream().anyMatch(grant -> itemId.equals(grant.itemId()));
    }

    record AssignmentPlan(
            boolean removeDisabledWatheGuns,
            boolean grantVigilanteGun,
            boolean grantProfessorIronManProtection,
            boolean grantBodyguardProtection,
            List<NoellesAssignedLoadouts.ItemGrant> assignmentItemGrants,
            List<NoellesAssignedLoadouts.UnsupportedItemGrant> unsupportedItemGrants
    ) {
        AssignmentPlan {
            assignmentItemGrants = List.copyOf(assignmentItemGrants);
            unsupportedItemGrants = List.copyOf(unsupportedItemGrants);
        }

        private static AssignmentPlan nothing() {
            return new AssignmentPlan(false, false, false, false, List.of(), List.of());
        }
    }
}
