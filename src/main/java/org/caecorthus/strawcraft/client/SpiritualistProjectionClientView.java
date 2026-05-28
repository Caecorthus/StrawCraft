package org.caecorthus.strawcraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.util.math.Vec3d;
import org.caecorthus.strawcraft.NoellesRoleState;
import org.caecorthus.strawcraft.NoellesRoleStateComponent;

import java.util.Optional;
import java.util.UUID;

public final class SpiritualistProjectionClientView {
    private static final double MAX_SPEED_PER_TICK = 0.45D;
    private static final double SPRINT_SPEED_MULTIPLIER = 1.6D;
    private static MarkerEntity marker;
    private static ClientWorld activeWorld;
    private static UUID activePlayerUuid;
    private static long activeStartedAtTick = Long.MIN_VALUE;
    private static boolean pendingCameraRestore;

    private SpiritualistProjectionClientView() {
    }

    public static void tick(MinecraftClient client) {
        restorePendingCamera(client);
        if (client.player == null || client.world == null) {
            cleanup(client);
            return;
        }

        Optional<NoellesRoleState.SpiritualistProjection> projection =
                NoellesRoleStateComponent.KEY.get(client.player).spiritualistProjection();
        if (projection.isEmpty()) {
            cleanup(client);
            return;
        }

        UUID playerUuid = client.player.getUuid();
        NoellesRoleState.SpiritualistProjection currentProjection = projection.get();
        if (marker == null
                || activeWorld != client.world
                || !playerUuid.equals(activePlayerUuid)
                || activeStartedAtTick != currentProjection.startedAtTick()) {
            cleanup(client);
            start(client, currentProjection, playerUuid);
        }

        moveMarker(client);
    }

    public static boolean isProjecting() {
        return marker != null;
    }

    private static void start(
            MinecraftClient client,
            NoellesRoleState.SpiritualistProjection projection,
            UUID playerUuid
    ) {
        marker = new MarkerEntity(EntityType.MARKER, client.world);
        marker.noClip = true;
        marker.setNoGravity(true);
        marker.setSilent(true);
        marker.setInvisible(true);
        marker.refreshPositionAndAngles(
                projection.bodyX(),
                client.player.getEyeY(),
                projection.bodyZ(),
                client.player.getYaw(),
                client.player.getPitch()
        );
        activeWorld = client.world;
        activePlayerUuid = playerUuid;
        activeStartedAtTick = projection.startedAtTick();
        pendingCameraRestore = false;
        // The marker is a client-only camera anchor; it is never spawned or synced as gameplay state.
        // Marker 只作为客户端相机锚点存在；它不会生成到服务端，也不会同步成玩法状态。
        client.setCameraEntity(marker);
    }

    private static void moveMarker(MinecraftClient client) {
        if (marker == null) {
            return;
        }

        syncMarkerLook(client);
        Vec3d movement = Vec3d.ZERO;
        Vec3d forward = Vec3d.fromPolar(0.0F, marker.getYaw());
        Vec3d right = Vec3d.fromPolar(0.0F, marker.getYaw() + 90.0F);
        if (client.options.forwardKey.isPressed()) {
            movement = movement.add(forward);
        }
        if (client.options.backKey.isPressed()) {
            movement = movement.subtract(forward);
        }
        if (client.options.rightKey.isPressed()) {
            movement = movement.add(right);
        }
        if (client.options.leftKey.isPressed()) {
            movement = movement.subtract(right);
        }
        if (client.options.jumpKey.isPressed()) {
            movement = movement.add(0.0D, 1.0D, 0.0D);
        }
        if (client.options.sneakKey.isPressed()) {
            movement = movement.subtract(0.0D, 1.0D, 0.0D);
        }

        if (movement.lengthSquared() > 1.0D) {
            movement = movement.normalize();
        }

        double speed = MAX_SPEED_PER_TICK;
        if (client.options.sprintKey.isPressed()) {
            speed *= SPRINT_SPEED_MULTIPLIER;
        }
        marker.setPosition(marker.getPos().add(movement.multiply(speed)));
    }

    private static void syncMarkerLook(MinecraftClient client) {
        if (marker == null || client.player == null) {
            return;
        }

        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();
        marker.setYaw(yaw);
        marker.setPitch(pitch);
        marker.prevYaw = client.player.prevYaw;
        marker.prevPitch = client.player.prevPitch;
        marker.setHeadYaw(yaw);
        marker.setBodyYaw(yaw);
    }

    private static void cleanup(MinecraftClient client) {
        restorePendingCamera(client);
        if (marker == null) {
            clearActiveProjection();
            return;
        }

        if (client.getCameraEntity() == marker) {
            if (client.player != null) {
                client.setCameraEntity(client.player);
            } else {
                pendingCameraRestore = true;
            }
        }
        marker.discard();
        marker = null;
        clearActiveProjection();
    }

    private static void restorePendingCamera(MinecraftClient client) {
        if (!pendingCameraRestore || client.player == null) {
            return;
        }

        if (client.getCameraEntity() != client.player) {
            client.setCameraEntity(client.player);
        }
        pendingCameraRestore = false;
    }

    private static void clearActiveProjection() {
        activeWorld = null;
        activePlayerUuid = null;
        activeStartedAtTick = Long.MIN_VALUE;
    }
}
