package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class VigilanteLoadout {
    public static final TaczGunProfile VIGILANTE_GUN = TaczGunProfiles.RHINO357;

    private VigilanteLoadout() {
    }

    static boolean shouldReplaceAssignedRole(Role role) {
        return StrawRoleMeaning.receivesVigilanteLoadout(role);
    }

    static void giveAssignedLoadout(PlayerEntity player) {
        ItemStack rhino357 = createRhino357Stack();
        if (rhino357.isEmpty()) {
            return;
        }

        // RoleAssignedLoadouts cleans disabled Wathe guns before granting this StrawCraft loadout.
        // RoleAssignedLoadouts 会先清掉被禁用的 Wathe 枪，再发放这套 StrawCraft 装备。
        player.giveItemStack(rhino357);
    }

    private static ItemStack createRhino357Stack() {
        return TaczGunStacks.createGunStack(VIGILANTE_GUN);
    }

    static NbtComponent createRhino357CustomData() {
        return TaczGunStacks.createGunCustomData(VIGILANTE_GUN);
    }
}
