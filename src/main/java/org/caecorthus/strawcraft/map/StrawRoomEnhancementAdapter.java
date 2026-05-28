package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.caecorthus.strawcraft.AttendantRoomManifest;
import org.caecorthus.strawcraft.StrawCraft;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StrawRoomEnhancementAdapter {
    private StrawRoomEnhancementAdapter() {
    }

    public static void applyInitializedRooms(ServerWorld world, GameWorldComponent gameComponent) {
        Optional<StrawMapEntry> map = mapFor(world, gameComponent);
        if (map.isEmpty() || map.get().rooms().isEmpty()) {
            return;
        }

        List<ServerPlayerEntity> participants = world.getPlayers().stream()
                .filter(player -> gameComponent.getRoles().containsKey(player.getUuid()))
                .toList();
        List<UUID> participantIds = participants.stream()
                .map(ServerPlayerEntity::getUuid)
                .toList();
        // Room assignment follows the current Wathe participant list and leaves overflow at the normal map spawn.
        // 房间分配只处理当前 Wathe 参赛者；容量溢出的玩家保留在普通地图出生点。
        Map<UUID, StrawRoomAssignment> assignments = StrawRoomAllocator.assign(participantIds, map.get().rooms());

        for (ServerPlayerEntity player : participants) {
            StrawRoomAssignment assignment = assignments.get(player.getUuid());
            if (assignment == null) {
                continue;
            }
            teleportToRoom(player, world, assignment.spawn());
            player.giveItemStack(StrawRoomKeyFactory.create(assignment.room().keyName()));
        }
        AttendantRoomManifest.giveToAssignedAttendants(participants, gameComponent.getRoles(), assignments, map.get().rooms());

        int overflow = participantIds.size() - assignments.size();
        if (overflow > 0) {
            StrawCraft.LOGGER.warn(
                    "{} players did not receive StrawCraft rooms on {} because room capacity was exhausted",
                    overflow,
                    map.get().id()
            );
        }
    }

    private static Optional<StrawMapEntry> mapFor(ServerWorld world, GameWorldComponent gameComponent) {
        return StrawCurrentMapResolver.resolve(world, gameComponent);
    }

    static Optional<StrawMapEntry> mapFor(Identifier dimensionId, Identifier gameModeId, Identifier mapEffectId) {
        return StrawCurrentMapResolver.resolve(dimensionId, gameModeId, mapEffectId);
    }

    private static void teleportToRoom(ServerPlayerEntity player, ServerWorld world, StrawRoomSpawnPoint spawn) {
        player.teleportTo(new TeleportTarget(
                world,
                new Vec3d(spawn.x(), spawn.y(), spawn.z()),
                Vec3d.ZERO,
                spawn.yaw(),
                spawn.pitch(),
                TeleportTarget.NO_OP
        ));
    }
}
