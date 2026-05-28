package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

final class ConductorMasterKeyAccess {
    static final Identifier CONDUCTOR_ROLE_ID = StrawCraft.id("conductor");

    private ConductorMasterKeyAccess() {
    }

    static boolean allowsLockedDoorAccess(Identifier roleId, boolean roundRunning, Iterable<Identifier> carriedItemIds) {
        return allowsLockedDoorAccess(CONDUCTOR_ROLE_ID.equals(roleId), roundRunning, carriedItemIds);
    }

    static boolean allowsLockedDoorAccess(boolean conductor, boolean roundRunning, Iterable<Identifier> carriedItemIds) {
        if (!roundRunning || !conductor || carriedItemIds == null) {
            return false;
        }
        for (Identifier itemId : carriedItemIds) {
            if (StrawCraftItems.MASTER_KEY_ID.equals(itemId)) {
                return true;
            }
        }
        return false;
    }
}
