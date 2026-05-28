package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import org.caecorthus.strawcraft.api.StrawDeathEvents;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScavengerHiddenBodies {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final ScavengerHiddenBodyState STATE = new ScavengerHiddenBodyState();

    private ScavengerHiddenBodies() {
    }

    public static void registerEvents() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(ScavengerHiddenBodies::recordRoleDeath);
        GameEvents.ON_FINISH_INITIALIZE.register((world, game) -> clearRoundWorld(world));
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> clearRoundWorld(world));
    }

    static void recordRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        if (!context.official().spawnBodyRequested() || !context.official().watheBaselineOwnsBodyAndSpectator()) {
            return;
        }

        if (context.killerRoleId().filter(ScavengerHiddenBodyVisibility.SCAVENGER_ROLE::equals).isEmpty()) {
            return;
        }

        STATE.recordHiddenBody(context.official().worldId(), context.official().victimUuid());
    }

    public static boolean isHiddenBody(ServerWorld world, PlayerBodyEntity body) {
        return STATE.isHiddenBody(world.getRegistryKey().getValue(), body.getPlayerUuid());
    }

    public static void clearConsumedBody(ServerWorld world, PlayerBodyEntity body) {
        // Vulture consumption removes the official body before round reset, so clear both server and client marks.
        // Vulture 吞尸会在回合重置前移除官方尸体，因此同时清掉服务端索引和客户端同步标记。
        STATE.clearHiddenBody(world.getRegistryKey().getValue(), body.getPlayerUuid());
        ((ScavengerHiddenBodyEntity) body).strawcraft$setHiddenByScavenger(false);
    }

    public static void markSpawnedBody(PlayerEntity victim, PlayerEntity killer, PlayerBodyEntity body) {
        if (victim.getWorld().isClient() || killer == null) {
            return;
        }

        Optional<net.minecraft.util.Identifier> killerRoleId = Optional.ofNullable(
                        GameWorldComponent.KEY.get(killer.getWorld()).getRole(killer)
                )
                .flatMap(StrawRoleMeaning::roleIdFor);
        if (killerRoleId.filter(ScavengerHiddenBodyVisibility.SCAVENGER_ROLE::equals).isEmpty()) {
            return;
        }

        // Server-side mark owns the round state; tracked data carries the render decision to clients.
        // 服务端记录负责回合状态；tracked data 只把渲染判断同步给客户端。
        STATE.recordHiddenBody(victim.getWorld().getRegistryKey().getValue(), victim.getUuid());
        ((ScavengerHiddenBodyEntity) body).strawcraft$setHiddenByScavenger(true);
    }

    private static void clearRoundWorld(net.minecraft.world.World world) {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getEntitiesByType(
                    TypeFilter.equals(PlayerBodyEntity.class),
                    new Box(-30_000_000.0D, -2_048.0D, -30_000_000.0D, 30_000_000.0D, 2_048.0D, 30_000_000.0D),
                    body -> true
            ).forEach(body -> ((ScavengerHiddenBodyEntity) body).strawcraft$setHiddenByScavenger(false));
            STATE.clearWorld(serverWorld.getRegistryKey().getValue());
        }
    }

    static void clearAll() {
        STATE.clearAll();
    }
}
