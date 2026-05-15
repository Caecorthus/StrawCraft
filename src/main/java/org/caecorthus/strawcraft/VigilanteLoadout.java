package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class VigilanteLoadout {
    public static final Identifier VIGILANTE_GUN_ID = Identifier.of("tacz", "rhino357");

    private VigilanteLoadout() {
    }

    public static void registerRoleAssignedHandler() {
        RoleAssigned.EVENT.register(VigilanteLoadout::replaceAssignedLoadout);
    }

    static boolean shouldReplaceAssignedRole(Role role) {
        return role == WatheRoles.VIGILANTE;
    }

    private static void replaceAssignedLoadout(PlayerEntity player, Role role) {
        if (!shouldReplaceAssignedRole(role) || player.getWorld().isClient()) {
            return;
        }

        ItemStack rhino357 = createRhino357Stack();
        if (rhino357.isEmpty()) {
            return;
        }

        // Wathe's own RoleAssigned handler grants a Wathe revolver to vigilantes first.
        // Replace that role loadout item only; killer shop entries are handled separately.
        removeFirstWatheRevolver(player);
        player.giveItemStack(rhino357);
    }

    private static ItemStack createRhino357Stack() {
        Item item = Registries.ITEM.get(TaczGunStacks.MODERN_KINETIC_GUN_ITEM_ID);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_DATA, createRhino357CustomData());
        return stack;
    }

    static NbtComponent createRhino357CustomData() {
        return TaczGunStacks.createGunCustomData(VIGILANTE_GUN_ID);
    }

    private static boolean removeFirstWatheRevolver(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isOf(WatheItems.REVOLVER)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    inventory.setStack(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }
}
