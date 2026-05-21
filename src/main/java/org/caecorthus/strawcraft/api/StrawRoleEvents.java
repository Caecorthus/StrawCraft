package org.caecorthus.strawcraft.api;

import dev.doctor4t.wathe.api.Role;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;

public final class StrawRoleEvents {
    public static final Event<RoleAssigned> ROLE_ASSIGNED = EventFactory.createArrayBacked(
            RoleAssigned.class,
            listeners -> (player, role) -> {
                for (RoleAssigned listener : listeners) {
                    listener.onRoleAssigned(player, role);
                }
            }
    );

    private StrawRoleEvents() {
    }

    public interface RoleAssigned {
        void onRoleAssigned(PlayerEntity player, Role role);
    }
}
