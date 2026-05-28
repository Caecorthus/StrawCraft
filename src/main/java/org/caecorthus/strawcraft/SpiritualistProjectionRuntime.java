package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SpiritualistProjectionRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private SpiritualistProjectionRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(SpiritualistProjectionPayload.ID, SpiritualistProjectionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SpiritualistProjectionPayload.ID, SpiritualistProjectionRuntime::handleProjection);
        ServerTickEvents.END_SERVER_TICK.register(SpiritualistProjectionRuntime::tickProjectingPlayers);
    }

    private static void handleProjection(SpiritualistProjectionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity spiritualist = context.player();
        ServerWorld world = spiritualist.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(spiritualist);

        // The client sends empty intent; role, round, life state, projection state, and cooldown stay server-owned.
        // 客户端只发送空意图；身份、回合、生存状态、投射状态和冷却都由服务端裁决。
        SpiritualistProjectionPolicy.Result result = SpiritualistProjectionPolicy.validateToggle(new SpiritualistProjectionPolicy.Input(
                game.isRunning(),
                StrawRoleMeaning.receivesSpiritualistProjection(game.getRole(spiritualist)),
                GameFunctions.isPlayerAliveAndSurvival(spiritualist),
                false,
                !roleState.isAbilityOnCooldown(SpiritualistProjectionPolicy.ABILITY_ID, currentGameTime),
                SpiritualistProjectionPolicy.isProjecting(roleState)
        ));

        switch (result) {
            case START_PROJECTING -> {
                SpiritualistProjectionPolicy.startProjecting(
                        roleState,
                        spiritualist.getX(),
                        spiritualist.getY(),
                        spiritualist.getZ(),
                        currentGameTime
                );
                spiritualist.sendMessage(Text.translatable("message.strawcraft.spiritualist.projecting")
                        .formatted(Formatting.AQUA), true);
            }
            case RETURN_TO_BODY -> {
                SpiritualistProjectionPolicy.returnToBody(roleState, currentGameTime);
                spiritualist.sendMessage(Text.translatable("message.strawcraft.spiritualist.returned")
                        .formatted(Formatting.AQUA), true);
            }
            case COOLDOWN -> spiritualist.sendMessage(Text.translatable(
                    "message.strawcraft.spiritualist.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            default -> {
            }
        }
    }

    public static boolean forceReturnAfterDamage(ServerPlayerEntity player, float damageAmount) {
        if (damageAmount <= 0.0F) {
            return false;
        }
        ServerWorld world = player.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!game.isRunning() || !isSpiritualistRole(game.getRole(player))) {
            return false;
        }
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(player);
        if (!SpiritualistProjectionPolicy.isProjecting(roleState)) {
            return false;
        }
        SpiritualistProjectionPolicy.forceReturn(roleState, world.getTime());
        player.sendMessage(Text.translatable("message.strawcraft.spiritualist.forced_return")
                .formatted(Formatting.YELLOW), true);
        return true;
    }

    public static boolean shouldSuppressSoundPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (!(packet instanceof PlaySoundS2CPacket) && !(packet instanceof PlaySoundFromEntityS2CPacket)) {
            return false;
        }
        ServerWorld world = player.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        return game.isRunning()
                && isSpiritualistRole(game.getRole(player))
                && SpiritualistProjectionPolicy.isProjecting(NoellesRoleStateComponent.KEY.get(player));
    }

    private static void tickProjectingPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickProjection(player);
        }
    }

    private static void tickProjection(ServerPlayerEntity player) {
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(player);
        NoellesRoleState.SpiritualistProjection projection = roleState.spiritualistProjection().orElse(null);
        if (projection == null) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        boolean shouldForceReturn = !game.isRunning()
                || !isSpiritualistRole(game.getRole(player))
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || SpiritualistProjectionPolicy.movedTooFarFromBody(
                projection,
                player.getX(),
                player.getY(),
                player.getZ()
        );
        if (shouldForceReturn) {
            SpiritualistProjectionPolicy.forceReturn(roleState, world.getTime());
            player.sendMessage(Text.translatable("message.strawcraft.spiritualist.forced_return")
                    .formatted(Formatting.YELLOW), true);
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(SpiritualistProjectionPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isSpiritualistRole(Role role) {
        return StrawRoleMeaning.receivesSpiritualistProjection(role);
    }
}
