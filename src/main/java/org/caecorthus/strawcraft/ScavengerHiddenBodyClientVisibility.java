package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class ScavengerHiddenBodyClientVisibility {
    private ScavengerHiddenBodyClientVisibility() {
    }

    public static boolean shouldRender(PlayerBodyEntity body, MinecraftClient client) {
        if (!((ScavengerHiddenBodyEntity) body).strawcraft$isHiddenByScavenger()) {
            return true;
        }

        ClientPlayerEntity viewer = client.player;
        if (viewer == null || client.world == null) {
            return false;
        }

        Role role = GameWorldComponent.KEY.get(viewer.getWorld()).getRole(viewer);
        return ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                viewer.isSpectator() || viewer.isCreative(),
                StrawRoleMeaning.roleIdFor(role),
                StrawRoleMeaning.factionFor(role)
        );
    }
}
