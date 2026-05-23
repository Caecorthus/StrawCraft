package org.caecorthus.strawcraft.map;

import com.mojang.brigadier.CommandDispatcher;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.caecorthus.strawcraft.StrawCraft;

import java.util.Optional;

public final class StrawMapVoting {
    private static final MapVotingEffectApplier EFFECT_APPLIER = new MapVotingEffectApplier();

    private StrawMapVoting() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(VotePayload.ID, VotePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VotePayload.ID, (payload, context) ->
                StrawMapVotingComponent.KEY.get(context.player().getServer().getScoreboard())
                        .castVote(context.player().getUuid(), payload.mapIndex()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                // Start voting after Wathe finalizes a round, before another game is allowed to begin.
                // 在 Wathe 完成一局收尾后立刻开始投票，赶在下一局被允许启动之前。
                StrawMapVotingComponent.KEY.get(serverWorld.getServer().getScoreboard()).startVoting(serverWorld);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> teleportJoiningPlayerToSelectedMap(handler.getPlayer(), server));
    }

    public static boolean isVotingActive(MinecraftServer server) {
        return component(server.getScoreboard()).isVotingActive();
    }

    static Identifier currentGameModeId(ServerWorld sourceWorld) {
        GameMode currentMode = GameWorldComponent.KEY.get(sourceWorld).getGameMode();
        return currentMode == null ? StrawMapEntry.DEFAULT_GAME_MODE : currentMode.identifier;
    }

    static Optional<MapVoteFinishPlan> finishPlan(MapVotingStateMachine.Transition transition) {
        return transition.selectedMap().map(MapVoteFinishPlan::from);
    }

    static void applyFinishEffects(MinecraftServer server, MapVotingStateMachine.Transition transition) {
        applyFinishEffects(server, transition, EFFECT_APPLIER);
    }

    static void applyFinishEffects(
            MinecraftServer server,
            MapVotingStateMachine.Transition transition,
            MapVotingEffectApplier effectApplier
    ) {
        finishPlan(transition).ifPresent(plan -> effectApplier.applyFinishEffects(server, plan));
    }

    private static void teleportJoiningPlayerToSelectedMap(ServerPlayerEntity player, MinecraftServer server) {
        StrawMapVotingComponent voting = component(server.getScoreboard());
        Identifier selectedDimension = voting.getLastSelectedDimension();
        if (selectedDimension == null) {
            return;
        }
        ServerWorld targetWorld = worldFor(server, selectedDimension);
        if (targetWorld != null && !GameWorldComponent.KEY.get(targetWorld).isRunning()) {
            teleportToMapSpawn(player, targetWorld);
        }
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("strawcraft:mapvote")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    StrawMapVotingComponent voting = component(context.getSource().getServer().getScoreboard());
                    voting.startVoting(context.getSource().getWorld());
                    return 1;
                }));
        dispatcher.register(CommandManager.literal("strawcraft:skipVote")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> component(context.getSource().getServer().getScoreboard()).skipWaitingPhase() ? 1 : 0));
    }

    private static StrawMapVotingComponent component(Scoreboard scoreboard) {
        return StrawMapVotingComponent.KEY.get(scoreboard);
    }

    private static ServerWorld worldFor(MinecraftServer server, Identifier dimensionId) {
        return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
    }

    private static void teleportToMapSpawn(ServerPlayerEntity player, ServerWorld targetWorld) {
        MapVariablesWorldComponent.PosWithOrientation spawn = MapVariablesWorldComponent.KEY.get(targetWorld).getSpawnPos();
        player.teleportTo(new TeleportTarget(
                targetWorld,
                spawn.pos,
                Vec3d.ZERO,
                spawn.yaw,
                spawn.pitch,
                TeleportTarget.NO_OP
        ));
    }

    public record VotePayload(int mapIndex) implements CustomPayload {
        public static final Id<VotePayload> ID = new Id<>(StrawCraft.id("map_vote"));
        public static final PacketCodec<RegistryByteBuf, VotePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT,
                VotePayload::mapIndex,
                VotePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
