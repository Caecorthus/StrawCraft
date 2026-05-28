package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.server.world.ServerWorld;
import org.caecorthus.strawcraft.api.StrawKillEvents;
import org.caecorthus.strawcraft.api.StrawRoleEvents;
import org.caecorthus.strawcraft.api.StrawShopEvents;
import org.caecorthus.strawcraft.map.StrawPlayerEnhancementAdapter;
import org.caecorthus.strawcraft.map.StrawRoomEnhancementAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WatheOfficialBridge {
    private WatheOfficialBridge() {
    }

    public static void register() {
        AllowPlayerDeath.EVENT.register((victim, killer, deathReason) ->
                !StrawKillEvents.BEFORE_KILL.invoker().beforeKill(victim, killer, deathReason).cancelWatheKill());
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                StrawCorpseMetadata.clearAll();
                DetectiveKillHistoryRuntime.resetAll();
                resetNoellesRoleState(serverWorld);
                Map<UUID, Role> officialRoles = Map.copyOf(gameComponent.getRoles());
                boolean rewritten = NoellesRuntimeRoleSelection.replaceEligibleOfficialAssignments(gameComponent);
                if (rewritten) {
                    announceNoellesRewrittenRoles(serverWorld, officialRoles, gameComponent.getRoles());
                }
                publishInitializedRoles(serverWorld, gameComponent);
                StrawRoomEnhancementAdapter.applyInitializedRooms(serverWorld, gameComponent);
                StrawPlayerEnhancementAdapter.applyInitializedPlayers(serverWorld, gameComponent);
            }
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                announceNoellesNeutralWinClaims(serverWorld, gameComponent);
                StrawCorpseMetadata.clearAll();
                DetectiveKillHistoryRuntime.resetAll();
                resetNoellesRoleState(serverWorld);
                StrawPlayerEnhancementAdapter.clearPlayers(serverWorld);
            }
        });
    }

    public static void rewriteGlobalShopEntries() {
        List<ShopEntry> originalEntries = List.copyOf(GameConstants.SHOP_ENTRIES);
        List<ShopEntry> rewrittenEntries = StrawShopEvents.buildEntries(originalEntries);
        if (originalEntries.equals(rewrittenEntries)) {
            return;
        }

        // Official Wathe buys by index from this global list, so StrawCraft rewrites it in place.
        // 官方 Wathe 会按 index 从这个全局列表购买，所以 StrawCraft 只做原地改写。
        GameConstants.SHOP_ENTRIES.clear();
        GameConstants.SHOP_ENTRIES.addAll(rewrittenEntries);
    }

    private static void announceNoellesRewrittenRoles(ServerWorld world, Map<UUID, Role> officialRoles, Map<UUID, Role> currentRoles) {
        currentRoles.forEach((playerUuid, role) -> {
            if (role == null || !roleChanged(officialRoles.get(playerUuid), role)) {
                return;
            }
            var player = world.getPlayerByUuid(playerUuid);
            if (player == null) {
                return;
            }
            NoellesRoleWelcome.Messages messages = NoellesRoleWelcome.messagesFor(role);
            player.sendMessage(messages.role(), false);
            player.sendMessage(messages.goal(), false);
            player.sendMessage(messages.actionbar(), true);
        });
    }

    private static boolean roleChanged(Role officialRole, Role currentRole) {
        if (officialRole == null || currentRole == null) {
            return officialRole != currentRole;
        }
        return !officialRole.identifier().equals(currentRole.identifier());
    }

    private static void publishInitializedRoles(ServerWorld world, GameWorldComponent gameComponent) {
        // This fires after official Wathe has finished assignment, avoiding addRole events during NBT restore.
        // 这里等官方 Wathe 分配完成后再统一发布，避免读档恢复角色时误触发装备逻辑。
        gameComponent.getRoles().forEach((playerUuid, role) -> {
            var player = world.getPlayerByUuid(playerUuid);
            if (player == null) {
                return;
            }
            StrawPlayerShopComponent shopState = StrawPlayerShopComponent.KEY.get(player);
            shopState.reset();
            StrawShopEvents.buildEntries(GameConstants.SHOP_ENTRIES)
                    .forEach(entry -> StrawShopEntry.metadata(entry)
                            .ifPresent(strawEntry -> shopState.ensureEntry(strawEntry, world.getTime())));
            shopState.sync();
            StrawRoleEvents.ROLE_ASSIGNED.invoker().onRoleAssigned(player, role);
        });
    }

    private static void announceNoellesNeutralWinClaims(ServerWorld world, GameWorldComponent gameComponent) {
        Map<UUID, Set<NoellesRoleState.NeutralWinClaim>> claimsByPlayer = new HashMap<>();
        gameComponent.getRoles().keySet().forEach(playerUuid -> {
            var player = world.getPlayerByUuid(playerUuid);
            if (player == null) {
                return;
            }
            Set<NoellesRoleState.NeutralWinClaim> claims = NoellesRoleStateComponent.KEY.get(player).neutralWinClaims();
            if (!claims.isEmpty()) {
                claimsByPlayer.put(playerUuid, claims);
            }
        });
        List<NoellesNeutralWinNotification.ClaimNotice> notices =
                NoellesNeutralWinNotification.collectClaims(claimsByPlayer);
        if (notices.isEmpty()) {
            return;
        }

        List<net.minecraft.server.network.ServerPlayerEntity> recipients = gameComponent.getRoles().keySet().stream()
                .map(world::getPlayerByUuid)
                .filter(net.minecraft.server.network.ServerPlayerEntity.class::isInstance)
                .map(net.minecraft.server.network.ServerPlayerEntity.class::cast)
                .toList();
        notices.forEach(notice -> {
            var claimant = world.getPlayerByUuid(notice.playerUuid());
            var claimantName = claimant == null ? net.minecraft.text.Text.literal(notice.playerUuid().toString()) : claimant.getDisplayName();
            NoellesNeutralWinNotification.Messages messages =
                    NoellesNeutralWinNotification.messagesFor(notice, claimantName);
            recipients.forEach(player -> player.sendMessage(messages.broadcast(), false));
            if (claimant != null) {
                claimant.sendMessage(messages.actionbar(), true);
            }
        });
    }

    private static void resetNoellesRoleState(ServerWorld world) {
        // Noelles role state is round-scoped; persistent player saves should not leak abilities between games.
        // Noelles 职业状态按回合计算；玩家存档里的旧技能状态不能漏到下一局。
        world.getPlayers().forEach(player -> NoellesRoleStateComponent.KEY.get(player).reset());
    }
}
