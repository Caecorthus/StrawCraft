package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.AllowPlayerOpenLockedDoor;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ConductorMasterKeyRuntime {
    private ConductorMasterKeyRuntime() {
    }

    public static void register() {
        AllowPlayerOpenLockedDoor.EVENT.register(ConductorMasterKeyRuntime::allowPlayerOpenLockedDoor);
    }

    private static boolean allowPlayerOpenLockedDoor(PlayerEntity player) {
        if (player == null || player.getWorld().isClient()) {
            return false;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        Role role = gameComponent.getRole(player);
        return ConductorMasterKeyAccess.allowsLockedDoorAccess(
                StrawRoleMeaning.matchesRoleId(role, ConductorMasterKeyAccess.CONDUCTOR_ROLE_ID),
                gameComponent.isRunning(),
                carriedItemIds(player)
        );
    }

    private static List<Identifier> carriedItemIds(PlayerEntity player) {
        List<Identifier> itemIds = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                itemIds.add(Registries.ITEM.getId(stack.getItem()));
            }
        }
        return itemIds;
    }
}
