package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.server.world.ServerWorld;
import org.caecorthus.strawcraft.api.StrawKillEvents;
import org.caecorthus.strawcraft.api.StrawRoleEvents;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.List;

public final class WatheOfficialBridge {
    private WatheOfficialBridge() {
    }

    public static void register() {
        AllowPlayerDeath.EVENT.register((victim, killer, deathReason) ->
                !StrawKillEvents.BEFORE_KILL.invoker().beforeKill(victim, killer, deathReason).cancelWatheKill());
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                publishInitializedRoles(serverWorld, gameComponent);
            }
        });
    }

    public static void rewriteGlobalShopEntries() {
        List<ShopEntry> originalEntries = List.copyOf(GameConstants.SHOP_ENTRIES);
        List<ShopEntry> rewrittenEntries = StrawShopEvents.modifyEntries(null, originalEntries);
        if (originalEntries.equals(rewrittenEntries)) {
            return;
        }

        // Official Wathe buys by index from this global list, so StrawCraft rewrites it in place.
        // 官方 Wathe 会按 index 从这个全局列表购买，所以 StrawCraft 只做原地改写。
        GameConstants.SHOP_ENTRIES.clear();
        GameConstants.SHOP_ENTRIES.addAll(rewrittenEntries);
    }

    private static void publishInitializedRoles(ServerWorld world, GameWorldComponent gameComponent) {
        // This fires after official Wathe has finished assignment, avoiding addRole events during NBT restore.
        // 这里等官方 Wathe 分配完成后再统一发布，避免读档恢复角色时误触发装备逻辑。
        gameComponent.getRoles().forEach((playerUuid, role) -> {
            var player = world.getPlayerByUuid(playerUuid);
            if (player == null) {
                return;
            }
            StrawRoleEvents.ROLE_ASSIGNED.invoker().onRoleAssigned(player, role);
        });
    }
}
