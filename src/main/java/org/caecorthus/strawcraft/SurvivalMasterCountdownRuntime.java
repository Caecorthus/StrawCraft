package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SurvivalMasterCountdownRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<Identifier, RoundRuntime> RUNTIMES = new HashMap<>();

    private SurvivalMasterCountdownRuntime() {
    }

    public static void registerEvents() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(SurvivalMasterCountdownRuntime::tickServer);
        GameEvents.ON_FINISH_INITIALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                initializeWorld(serverWorld, game);
            }
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                resetWorld(serverWorld);
            }
        });
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getTime() % 20 != 0) {
                continue;
            }
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!game.isRunning()) {
            return;
        }

        RoundRuntime runtime = runtimeFor(world);
        SurvivalMasterCountdownPolicy.Observation observation = observe(world, game, runtime.startingKillerCount);
        SurvivalMasterCountdownState.Update update = runtime.state.tick(observation, () -> endRoundAsPassengers(world));
        notifyPlayers(world, update);
    }

    private static SurvivalMasterCountdownPolicy.Observation observe(
            ServerWorld world,
            GameWorldComponent game,
            int startingKillerCount
    ) {
        int livingPlayerCount = 0;
        boolean survivalMasterAlive = false;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            livingPlayerCount++;
            Role role = game.getRole(player);
            if (StrawRoleMeaning.receivesSurvivalMasterCountdown(role)) {
                survivalMasterAlive = true;
            }
        }
        return new SurvivalMasterCountdownPolicy.Observation(
                startingKillerCount,
                livingPlayerCount,
                survivalMasterAlive
        );
    }

    private static void initializeWorld(ServerWorld world, GameWorldComponent game) {
        RoundRuntime runtime = runtimeFor(world);
        runtime.state.reset();
        runtime.startingKillerCount = game.getAllKillerTeamPlayers().size();
    }

    static void resetWorld(ServerWorld world) {
        RoundRuntime runtime = RUNTIMES.remove(world.getRegistryKey().getValue());
        if (runtime != null) {
            runtime.state.reset();
        }
    }

    private static RoundRuntime runtimeFor(ServerWorld world) {
        return RUNTIMES.computeIfAbsent(
                world.getRegistryKey().getValue(),
                unused -> new RoundRuntime(GameWorldComponent.KEY.get(world).getAllKillerTeamPlayers().size())
        );
    }

    private static void endRoundAsPassengers(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            return;
        }

        // Official Wathe owns the win screen data and stopping transition.
        // 官方 Wathe 继续负责胜利界面数据和停止游戏的状态切换。
        // 官方 Wathe 负责胜利界面数据和停止游戏的状态转换。
        GameRoundEndComponent.KEY.get(world).setRoundEndData(world.getPlayers(), GameFunctions.WinStatus.PASSENGERS);
        GameFunctions.stopGame(world);
    }

    private static void notifyPlayers(ServerWorld world, SurvivalMasterCountdownState.Update update) {
        if (update.started()) {
            broadcast(world, Text.literal("Survival Master countdown started: "
                    + SurvivalMasterCountdownState.COUNTDOWN_SECONDS + " seconds.").formatted(Formatting.GOLD));
            notifySurvivalMasters(world, Text.literal("Survive for " + update.remainingSeconds() + " seconds.")
                    .formatted(Formatting.YELLOW));
            return;
        }
        if (update.cancelled()) {
            broadcast(world, Text.literal("Survival Master countdown cancelled.").formatted(Formatting.RED));
            return;
        }
        if (update.completed()) {
            broadcast(world, Text.literal("Survival Master survived. Passengers win!").formatted(Formatting.GREEN));
            return;
        }
        if (update.progress() && shouldAnnounceProgress(update.remainingSeconds())) {
            Text message = Text.literal("Survival Master countdown: "
                    + update.remainingSeconds() + " seconds remaining.").formatted(Formatting.YELLOW);
            broadcast(world, message);
            notifySurvivalMasters(world, message);
        }
    }

    private static boolean shouldAnnounceProgress(int remainingSeconds) {
        return remainingSeconds == 60
                || remainingSeconds == 30
                || remainingSeconds == 10
                || remainingSeconds <= 5;
    }

    private static void broadcast(ServerWorld world, Text message) {
        world.getPlayers().forEach(player -> player.sendMessage(message, false));
    }

    private static void notifySurvivalMasters(ServerWorld world, Text message) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            Role role = game.getRole(player);
            if (StrawRoleMeaning.receivesSurvivalMasterCountdown(role)) {
                player.sendMessage(message, true);
            }
        }
    }

    private static final class RoundRuntime {
        private final SurvivalMasterCountdownState state = new SurvivalMasterCountdownState();
        private int startingKillerCount;

        private RoundRuntime(int startingKillerCount) {
            this.startingKillerCount = startingKillerCount;
        }
    }
}
