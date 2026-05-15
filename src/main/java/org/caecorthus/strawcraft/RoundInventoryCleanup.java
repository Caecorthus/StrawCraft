package org.caecorthus.strawcraft;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public final class RoundInventoryCleanup {
    private RoundInventoryCleanup() {
    }

    public static int removeDisabledWatheGuns(PlayerInventory inventory) {
        return removeDisabledWatheGuns(new PlayerSlotInventory(inventory), WeaponBalance::isDisabledWatheGun);
    }

    static <T> int removeDisabledWatheGuns(SlotInventory<T> inventory, Predicate<T> disabledWatheGunPredicate) {
        int removed = 0;
        for (int slot = 0; slot < inventory.slotCount(); slot++) {
            T stack = inventory.stackAt(slot);
            if (stack != null && disabledWatheGunPredicate.test(stack)) {
                inventory.clearSlot(slot);
                removed++;
            }
        }
        return removed;
    }

    interface SlotInventory<T> {
        int slotCount();

        T stackAt(int slot);

        void clearSlot(int slot);
    }

    private record PlayerSlotInventory(PlayerInventory inventory) implements SlotInventory<ItemStack> {
        @Override
        public int slotCount() {
            return inventory.size();
        }

        @Override
        public ItemStack stackAt(int slot) {
            return inventory.getStack(slot);
        }

        @Override
        public void clearSlot(int slot) {
            inventory.setStack(slot, ItemStack.EMPTY);
        }
    }
}
