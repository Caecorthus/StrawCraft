package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import net.minecraft.entity.player.PlayerEntity;

public final class RoleAssignedLoadouts {
    private RoleAssignedLoadouts() {
    }

    public static void register() {
        RoleAssigned.EVENT.register(RoleAssignedLoadouts::applyAssignedLoadout);
    }

    static AssignmentPlan planAssignedLoadout(Role role, boolean clientWorld) {
        if (clientWorld) {
            return AssignmentPlan.nothing();
        }
        return new AssignmentPlan(true, VigilanteLoadout.shouldReplaceAssignedRole(role));
    }

    private static void applyAssignedLoadout(PlayerEntity player, Role role) {
        AssignmentPlan plan = planAssignedLoadout(role, player.getWorld().isClient());
        if (plan.removeDisabledWatheGuns()) {
            RoundInventoryCleanup.removeDisabledWatheGuns(player.getInventory());
        }
        if (plan.grantVigilanteGun()) {
            VigilanteLoadout.giveAssignedLoadout(player);
        }
    }

    record AssignmentPlan(boolean removeDisabledWatheGuns, boolean grantVigilanteGun) {
        private static AssignmentPlan nothing() {
            return new AssignmentPlan(false, false);
        }
    }
}
