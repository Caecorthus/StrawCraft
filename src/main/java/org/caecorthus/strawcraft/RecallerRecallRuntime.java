package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RecallerRecallRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private RecallerRecallRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(RecallerRecallPayload.ID, RecallerRecallPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RecallerRecallPayload.ID, RecallerRecallRuntime::handleRecall);
    }

    private static void handleRecall(RecallerRecallPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity recaller = context.player();
        ServerWorld world = recaller.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(recaller);
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(recaller);
        Optional<NoellesRoleState.RecallPoint> recallPoint = roleState.recallerRecallPoint();

        // The client sends only intent; all role, round, cooldown, balance, and dimension checks stay server-owned.
        // 客户端只发送意图；身份、回合、冷却、余额和维度校验全部由服务端负责。
        RecallerRecallPolicy.Result result = RecallerRecallPolicy.validateActivation(new RecallerRecallPolicy.Input(
                game.isRunning(),
                isRecallerRole(game.getRole(recaller)),
                GameFunctions.isPlayerAliveAndSurvival(recaller),
                !roleState.isAbilityOnCooldown(RecallerRecallPolicy.ABILITY_ID, currentGameTime),
                recallPoint.isPresent(),
                recallPoint.map(point -> point.worldId().equals(world.getRegistryKey().getValue())).orElse(true),
                shop.balance
        ));

        switch (result) {
            case STORE_POINT -> storeRecallPoint(recaller, world, roleState, currentGameTime);
            case RECALL -> recallToStoredPoint(recaller, world, roleState, shop, recallPoint.orElseThrow(), currentGameTime);
            case COOLDOWN -> recaller.sendMessage(Text.translatable(
                    "message.strawcraft.recaller.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case WRONG_DIMENSION -> recaller.sendMessage(
                    Text.translatable("message.strawcraft.recaller.wrong_dimension").formatted(Formatting.RED),
                    true
            );
            case INSUFFICIENT_BALANCE -> recaller.sendMessage(
                    Text.translatable("message.strawcraft.recaller.insufficient_balance", RecallerRecallPolicy.RECALL_PRICE)
                            .formatted(Formatting.RED),
                    true
            );
            default -> {
            }
        }
    }

    private static void storeRecallPoint(
            ServerPlayerEntity recaller,
            ServerWorld world,
            NoellesRoleStateComponent roleState,
            long currentGameTime
    ) {
        roleState.setRecallerRecallPoint(new NoellesRoleState.RecallPoint(
                world.getRegistryKey().getValue(),
                recaller.getX(),
                recaller.getY(),
                recaller.getZ(),
                recaller.getYaw(),
                recaller.getPitch()
        ));
        roleState.tryBeginAbilityCooldown(
                RecallerRecallPolicy.ABILITY_ID,
                currentGameTime,
                RecallerRecallPolicy.STORE_COOLDOWN_TICKS
        );
        recaller.sendMessage(Text.translatable("message.strawcraft.recaller.stored").formatted(Formatting.AQUA), true);
    }

    private static void recallToStoredPoint(
            ServerPlayerEntity recaller,
            ServerWorld world,
            NoellesRoleStateComponent roleState,
            PlayerShopComponent shop,
            NoellesRoleState.RecallPoint point,
            long currentGameTime
    ) {
        shop.balance -= RecallerRecallPolicy.RECALL_PRICE;
        shop.sync();
        if (recaller.hasVehicle()) {
            recaller.stopRiding();
        }
        recaller.teleport(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        roleState.clearRecallerRecallPoint();
        roleState.tryBeginAbilityCooldown(
                RecallerRecallPolicy.ABILITY_ID,
                currentGameTime,
                RecallerRecallPolicy.RECALL_COOLDOWN_TICKS
        );
        recaller.sendMessage(Text.translatable("message.strawcraft.recaller.recalled").formatted(Formatting.GREEN), true);
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(RecallerRecallPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isRecallerRole(Role role) {
        return StrawRoleMeaning.receivesRecallerRecall(role);
    }
}
