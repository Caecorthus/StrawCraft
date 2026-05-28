package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ReporterMarkRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ReporterMarkRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(ReporterMarkPayload.ID, ReporterMarkPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReporterMarkPayload.ID, ReporterMarkRuntime::handleMark);
        ServerTickEvents.END_SERVER_TICK.register(ReporterMarkRuntime::tickServer);
    }

    private static void handleMark(ReporterMarkPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity reporter = context.player();
        ServerWorld world = reporter.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(reporter);
        ServerPlayerEntity target = reporter.getServer().getPlayerManager().getPlayer(payload.target());
        boolean sameWorld = target != null && target.getServerWorld() == world;

        // Clients propose one UUID; the server owns role, round, distance, visibility, and cooldown.
        // 客户端只提出一个 UUID；身份、回合、距离、可见性和冷却都由服务端裁决。
        ReporterMarkPolicy.ValidationResult result = ReporterMarkPolicy.validate(new ReporterMarkPolicy.InteractionInput(
                game.isRunning(),
                isReporterRole(game.getRole(reporter)),
                GameFunctions.isPlayerAliveAndSurvival(reporter),
                target != null,
                target == reporter,
                target != null && game.getRole(target) != null,
                target != null && GameFunctions.isPlayerAliveAndSurvival(target),
                sameWorld,
                target == null ? Double.POSITIVE_INFINITY : reporter.squaredDistanceTo(target),
                target != null && reporter.canSee(target),
                !roleState.isAbilityOnCooldown(ReporterMarkPolicy.ABILITY_ID, currentGameTime)
        ));
        if (result.blocked()) {
            sendBlockedMessage(reporter, roleState, currentGameTime, result);
            return;
        }

        roleState.setReporterMarkedTarget(target.getUuid());
        roleState.tryBeginAbilityCooldown(
                ReporterMarkPolicy.ABILITY_ID,
                currentGameTime,
                ReporterMarkPolicy.MARK_COOLDOWN_TICKS
        );
        reporter.sendMessage(Text.translatable(
                "message.strawcraft.reporter.marked",
                target.getDisplayName()
        ).formatted(Formatting.GREEN), true);
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getTime() % ReporterMarkPolicy.TRACKER_INTERVAL_TICKS != 0) {
                continue;
            }
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity reporter : world.getPlayers()) {
            tickReporterTracker(world, game, reporter);
        }
    }

    private static void tickReporterTracker(ServerWorld world, GameWorldComponent game, ServerPlayerEntity reporter) {
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(reporter);
        roleState.reporterMarkedTarget().ifPresent(targetUuid -> {
            ServerPlayerEntity target = reporter.getServer().getPlayerManager().getPlayer(targetUuid);
            if (!canTrackMarkedTarget(world, game, reporter, target)) {
                roleState.clearReporterMarkedTarget();
                return;
            }

            int distance = (int) Math.round(Math.sqrt(reporter.squaredDistanceTo(target)));
            reporter.sendMessage(Text.translatable(
                    "message.strawcraft.reporter.tracking",
                    target.getDisplayName(),
                    distance
            ).formatted(Formatting.AQUA), true);
        });
    }

    private static boolean canTrackMarkedTarget(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity reporter,
            ServerPlayerEntity target
    ) {
        return game.isRunning()
                && isReporterRole(game.getRole(reporter))
                && GameFunctions.isPlayerAliveAndSurvival(reporter)
                && target != null
                && target.getServerWorld() == world
                && game.getRole(target) != null
                && GameFunctions.isPlayerAliveAndSurvival(target);
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity reporter,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            ReporterMarkPolicy.ValidationResult result
    ) {
        switch (result) {
            case COOLDOWN -> reporter.sendMessage(Text.translatable(
                    "message.strawcraft.reporter.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case WRONG_DIMENSION -> reporter.sendMessage(
                    Text.translatable("message.strawcraft.reporter.wrong_dimension").formatted(Formatting.RED),
                    true
            );
            case TARGET_OUT_OF_REACH -> reporter.sendMessage(
                    Text.translatable("message.strawcraft.reporter.out_of_reach").formatted(Formatting.RED),
                    true
            );
            case INVALID_TARGET, TARGET_NOT_ACTIVE -> reporter.sendMessage(
                    Text.translatable("message.strawcraft.reporter.invalid_target").formatted(Formatting.RED),
                    true
            );
            default -> {
            }
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(ReporterMarkPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isReporterRole(Role role) {
        return StrawRoleMeaning.receivesReporterMark(role);
    }
}
