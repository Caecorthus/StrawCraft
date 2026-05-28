package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CanSeePoison;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class ToxicologistPoisonVisibility {
    private ToxicologistPoisonVisibility() {
    }

    public static void registerEvents() {
        CanSeePoison.EVENT.register(ToxicologistPoisonVisibility::canViewerSeePoison);
    }

    static boolean canSeePoison(@Nullable Role role) {
        return StrawRoleMeaning.receivesPoisonVisibility(role);
    }

    private static boolean canViewerSeePoison(PlayerEntity viewer) {
        if (viewer == null) {
            return false;
        }
        try {
            // Ask official Wathe for the viewer role, then keep addon poison-vision roles local.
            // 先向官方 Wathe 查询观察者职业，再在 StrawCraft 本地决定哪些附加职业能看见毒。
            // 先向官方 Wathe 查询观察者职业，再在 addon 侧判定可见毒物的职业。
            // 先向官方 Wathe 读取观察者职业，再在 StrawCraft 侧判断 Toxicologist 权限。
            Role role = GameWorldComponent.KEY.get(viewer.getWorld()).getRole(viewer);
            return canSeePoison(role);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
