package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundInventoryCleanupTest {
    @Test
    void removesEveryDisabledWatheGunFromInventorySlots() {
        FakeSlotInventory inventory = new FakeSlotInventory("knife", "revolver", "derringer", "p320", "revolver");

        int removed = RoundInventoryCleanup.removeDisabledWatheGuns(
                inventory,
                stack -> "revolver".equals(stack) || "derringer".equals(stack)
        );

        assertEquals(3, removed);
        assertEquals(Arrays.asList("knife", null, null, "p320", null), inventory.stacks);
    }

    private static final class FakeSlotInventory implements RoundInventoryCleanup.SlotInventory<String> {
        private final List<String> stacks;

        private FakeSlotInventory(String... stacks) {
            this.stacks = new ArrayList<>(List.of(stacks));
        }

        @Override
        public int slotCount() {
            return stacks.size();
        }

        @Override
        public String stackAt(int slot) {
            return stacks.get(slot);
        }

        @Override
        public void clearSlot(int slot) {
            stacks.set(slot, null);
        }
    }
}
