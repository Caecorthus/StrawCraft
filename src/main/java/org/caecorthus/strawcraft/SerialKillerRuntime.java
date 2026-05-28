package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawRoleEvents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SerialKillerRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int CHECK_INTERVAL_TICKS = 20;

    private SerialKillerRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        StrawRoleEvents.ROLE_ASSIGNED.register(SerialKillerRuntime::handleRoleAssigned);
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(SerialKillerRuntime::handleRoleDeath);
        ServerTickEvents.END_SERVER_TICK.register(SerialKillerRuntime::tickServer);
    }

    private static void handleRoleAssigned(net.minecraft.entity.player.PlayerEntity player, Role role) {
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(player);
        if (!(player instanceof ServerPlayerEntity serialKiller)
                || !StrawRoleMeaning.receivesSerialKillerTargeting(role)) {
            clearTargetIfPresent(roleState);
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(serialKiller.getServerWorld());
        refreshTarget(serialKiller.getServerWorld(), game, serialKiller, roleState);
    }

    private static void handleRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        ServerWorld world = context.world();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        // Pay before reassignment so the dead victim can still match the persisted target.
        // 先发奖励再重分配，确保死亡玩家仍能匹配持久化的当前目标。
        context.official().killerUuid().ifPresent(killerUuid -> payTargetBonus(world, game, context, killerUuid));
        for (ServerPlayerEntity serialKiller : world.getPlayers()) {
            tickSerialKiller(world, game, serialKiller);
        }
    }

    private static void payTargetBonus(
            ServerWorld world,
            GameWorldComponent game,
            StrawDeathEvents.RoleDeathContext context,
            UUID killerUuid
    ) {
        ServerPlayerEntity serialKiller = world.getServer().getPlayerManager().getPlayer(killerUuid);
        if (serialKiller == null) {
            return;
        }

        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(serialKiller);
        SerialKillerTargetPolicy.bonusGrant(
                killerUuid,
                context.official().victimUuid(),
                roleState.serialKillerCurrentTarget(),
                StrawRoleMeaning.receivesSerialKillerTargeting(game.getRole(killerUuid))
        ).ifPresent(grant -> KillRewardPayout.apply(
                List.of(grant),
                recipientUuid -> bonusAccount(world.getServer(), recipientUuid)
        ));
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getTime() % CHECK_INTERVAL_TICKS != 0) {
                continue;
            }
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity serialKiller : world.getPlayers()) {
            tickSerialKiller(world, game, serialKiller);
        }
    }

    private static void tickSerialKiller(ServerWorld world, GameWorldComponent game, ServerPlayerEntity serialKiller) {
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(serialKiller);
        Role role = game.getRole(serialKiller);
        if (!game.isRunning()
                || !StrawRoleMeaning.receivesSerialKillerTargeting(role)
                || !GameFunctions.isPlayerAliveAndSurvival(serialKiller)) {
            clearTargetIfPresent(roleState);
            return;
        }

        Optional<UUID> currentTarget = roleState.serialKillerCurrentTarget();
        List<SerialKillerTargetPolicy.TargetCandidate> candidates = targetCandidates(world, game, serialKiller);
        if (currentTarget.filter(targetUuid -> SerialKillerTargetPolicy.isTargetValid(targetUuid, candidates)).isPresent()) {
            return;
        }
        refreshTarget(candidates, roleState);
    }

    private static void refreshTarget(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity serialKiller,
            NoellesRoleStateComponent roleState
    ) {
        refreshTarget(targetCandidates(world, game, serialKiller), roleState);
    }

    private static void refreshTarget(
            List<SerialKillerTargetPolicy.TargetCandidate> candidates,
            NoellesRoleStateComponent roleState
    ) {
        SerialKillerTargetPolicy.assignTarget(candidates, ThreadLocalRandom.current())
                .ifPresentOrElse(
                        targetUuid -> roleState.setSerialKillerCurrentTarget(targetUuid),
                        () -> clearTargetIfPresent(roleState)
                );
    }

    private static void clearTargetIfPresent(NoellesRoleStateComponent roleState) {
        if (roleState.serialKillerCurrentTarget().isPresent()) {
            roleState.clearSerialKillerCurrentTarget();
        }
    }

    private static List<SerialKillerTargetPolicy.TargetCandidate> targetCandidates(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity serialKiller
    ) {
        return game.getRoles().keySet().stream()
                .map(playerUuid -> targetCandidate(world, game, serialKiller, playerUuid))
                .toList();
    }

    private static SerialKillerTargetPolicy.TargetCandidate targetCandidate(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity serialKiller,
            UUID playerUuid
    ) {
        net.minecraft.entity.player.PlayerEntity candidate = world.getPlayerByUuid(playerUuid);
        Role role = game.getRole(playerUuid);
        return new SerialKillerTargetPolicy.TargetCandidate(
                playerUuid,
                playerUuid.equals(serialKiller.getUuid()),
                role != null,
                candidate != null && GameFunctions.isPlayerAliveAndSurvival(candidate),
                // Taotie swallowing has no StrawCraft runtime yet; keep the policy slot explicit for that future source rule.
                // 饕餮吞噬运行时尚未迁移；这里保留显式字段，方便后续接上源规则。
                false,
                StrawRoleMeaning.canUseKillerShop(role),
                StrawRoleMeaning.receivesUndercoverWalkieTalkie(role),
                StrawRoleMeaning.receivesBodyguardProtection(role),
                StrawRoleMeaning.receivesSurvivalMasterCountdown(role)
        );
    }

    private static KillRewardPayout.Account bonusAccount(MinecraftServer server, UUID recipientUuid) {
        ServerPlayerEntity recipient = server.getPlayerManager().getPlayer(recipientUuid);
        if (recipient == null) {
            return null;
        }
        dev.doctor4t.wathe.cca.PlayerShopComponent shop = dev.doctor4t.wathe.cca.PlayerShopComponent.KEY.get(recipient);
        return new KillRewardPayout.Account() {
            @Override
            public int balance() {
                return shop.balance;
            }

            @Override
            public void setBalance(int balance) {
                shop.balance = balance;
            }

            @Override
            public void sync() {
                shop.sync();
            }
        };
    }
}
