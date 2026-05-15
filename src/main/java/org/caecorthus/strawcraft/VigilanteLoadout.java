package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class VigilanteLoadout {
    public static final TaczGunProfile VIGILANTE_GUN = TaczGunProfiles.RHINO357;

    private VigilanteLoadout() {
    }

    static boolean shouldReplaceAssignedRole(Role role) {
        return role == WatheRoles.VIGILANTE;
    }

    static void giveAssignedLoadout(PlayerEntity player) {
        ItemStack rhino357 = createRhino357Stack();
        if (rhino357.isEmpty()) {
            return;
        }

        // RoleAssignedLoadouts cleans disabled Wathe guns before granting this StrawCraft loadout.
        player.giveItemStack(rhino357);
    }

    private static ItemStack createRhino357Stack() {
        return TaczGunStacks.createGunStack(VIGILANTE_GUN);
    }

    static NbtComponent createRhino357CustomData() {
        return TaczGunStacks.createGunCustomData(VIGILANTE_GUN);
    }
}
