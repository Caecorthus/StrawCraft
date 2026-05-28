package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SwapperSwapRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private SwapperSwapRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(SwapperSwapPayload.ID, SwapperSwapPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SwapperSwapPayload.ID, SwapperSwapRuntime::handleSwap);
    }

    private static void handleSwap(SwapperSwapPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity swapper = context.player();
        ServerWorld world = swapper.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(swapper);
        ServerPlayerEntity targetA = swapper.getServer().getPlayerManager().getPlayer(payload.targetA());
        ServerPlayerEntity targetB = swapper.getServer().getPlayerManager().getPlayer(payload.targetB());
        boolean sameWorld = targetA != null && targetB != null
                && targetA.getServerWorld() == world && targetB.getServerWorld() == world;

        // Clients propose only UUIDs; role, round, cooldown, dimension, and safety stay server-owned.
        // 客户端只提出 UUID；职业、回合、冷却、维度和安全性都由服务端裁决。
        SwapperSwapPolicy.ValidationResult result = SwapperSwapPolicy.validate(new SwapperSwapPolicy.InteractionInput(
                game.isRunning(),
                isSwapperRole(game.getRole(swapper)),
                GameFunctions.isPlayerAliveAndSurvival(swapper),
                targetA != null,
                targetB != null,
                targetA == swapper,
                targetB == swapper,
                targetA != null && targetA == targetB,
                targetA != null && game.getRole(targetA) != null,
                targetB != null && game.getRole(targetB) != null,
                targetA != null && GameFunctions.isPlayerAliveAndSurvival(targetA),
                targetB != null && GameFunctions.isPlayerAliveAndSurvival(targetB),
                sameWorld,
                !roleState.isAbilityOnCooldown(SwapperSwapPolicy.ABILITY_ID, currentGameTime),
                sameWorld && isDestinationSafe(world, targetA, Position.of(targetB)),
                sameWorld && isDestinationSafe(world, targetB, Position.of(targetA))
        ));
        if (result.blocked()) {
            sendBlockedMessage(swapper, roleState, currentGameTime, result);
            return;
        }

        Position aPosition = Position.of(targetA);
        Position bPosition = Position.of(targetB);
        dismount(targetA);
        dismount(targetB);
        targetA.teleport(world, bPosition.x(), bPosition.y(), bPosition.z(), bPosition.yaw(), bPosition.pitch());
        targetB.teleport(world, aPosition.x(), aPosition.y(), aPosition.z(), aPosition.yaw(), aPosition.pitch());
        roleState.tryBeginAbilityCooldown(
                SwapperSwapPolicy.ABILITY_ID,
                currentGameTime,
                SwapperSwapPolicy.SWAP_COOLDOWN_TICKS
        );
        swapper.sendMessage(Text.translatable("message.strawcraft.swapper.swapped").formatted(Formatting.GREEN), true);
    }

    private static void dismount(ServerPlayerEntity player) {
        if (player.hasVehicle()) {
            player.stopRiding();
        }
    }

    private static boolean isDestinationSafe(ServerWorld world, ServerPlayerEntity target, Position destination) {
        if (!world.getWorldBorder().contains(BlockPos.ofFloored(destination.x(), destination.y(), destination.z()))) {
            return false;
        }
        Vec3d offset = new Vec3d(
                destination.x() - target.getX(),
                destination.y() - target.getY(),
                destination.z() - target.getZ()
        );
        Box destinationBox = target.getBoundingBox().offset(offset);
        // Check block collision only; the other target currently occupies the swap destination.
        // 这里只检查方块碰撞；另一个目标当前正站在交换终点上。
        return !world.getBlockCollisions(target, destinationBox).iterator().hasNext();
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity swapper,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            SwapperSwapPolicy.ValidationResult result
    ) {
        switch (result) {
            case COOLDOWN -> swapper.sendMessage(Text.translatable(
                    "message.strawcraft.swapper.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case WRONG_DIMENSION -> swapper.sendMessage(
                    Text.translatable("message.strawcraft.swapper.wrong_dimension").formatted(Formatting.RED),
                    true
            );
            case UNSAFE_DESTINATION -> swapper.sendMessage(
                    Text.translatable("message.strawcraft.swapper.unsafe").formatted(Formatting.RED),
                    true
            );
            case INVALID_TARGETS, TARGET_NOT_ACTIVE -> swapper.sendMessage(
                    Text.translatable("message.strawcraft.swapper.invalid_targets").formatted(Formatting.RED),
                    true
            );
            default -> {
            }
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(SwapperSwapPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isSwapperRole(Role role) {
        return StrawRoleMeaning.receivesSwapperSwap(role);
    }

    private record Position(double x, double y, double z, float yaw, float pitch) {
        private static Position of(ServerPlayerEntity player) {
            return new Position(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        }
    }
}
