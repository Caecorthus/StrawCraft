package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawRoleEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TaotieSwallowRuntime {
    public static final net.minecraft.util.Identifier SWALLOWED_ALL_TRIGGER = StrawCraft.id("taotie_swallowed_all");

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<UUID, UUID> SWALLOWED_BY_TARGET = new HashMap<>();
    private static final Map<UUID, Set<UUID>> TARGETS_BY_TAOTIE = new HashMap<>();

    private TaotieSwallowRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(TaotieSwallowPayload.ID, TaotieSwallowPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TaotieSwallowPayload.ID, TaotieSwallowRuntime::handleSwallow);
        ServerTickEvents.END_SERVER_TICK.register(TaotieSwallowRuntime::tickServer);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> releaseDisconnected(handler.getPlayer(), server));
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(TaotieSwallowRuntime::handleRoleDeath);
        StrawRoleEvents.ROLE_ASSIGNED.register(TaotieSwallowRuntime::handleRoleAssigned);
        StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register(TaotieSwallowRuntime::collectWinContributions);
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                releaseAllInWorld(serverWorld);
            }
        });
    }

    private static void handleSwallow(TaotieSwallowPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity taotie = context.player();
        ServerWorld world = taotie.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent taotieState = NoellesRoleStateComponent.KEY.get(taotie);
        ServerPlayerEntity target = taotie.getServer().getPlayerManager().getPlayer(payload.target());
        boolean sameWorld = target != null && target.getServerWorld() == world;

        // Clients send only the aimed UUID; role, life, cooldown, distance, and visibility stay server-owned.
        // 客户端只发送准星目标 UUID；身份、生存、冷却、距离和可见性都由服务端裁决。
        TaotieSwallowPolicy.ValidationResult result = TaotieSwallowPolicy.validateSwallow(
                new TaotieSwallowPolicy.SwallowInput(
                        game.isRunning(),
                        isTaotieRole(game.getRole(taotie)),
                        GameFunctions.isPlayerAliveAndSurvival(taotie),
                        taotieState.isTaotieSwallowed(),
                        !taotieState.isAbilityOnCooldown(TaotieSwallowPolicy.ABILITY_ID, currentGameTime),
                        target != null,
                        target == taotie,
                        target != null && game.getRole(target) != null,
                        target != null && GameFunctions.isPlayerAliveAndSurvival(target),
                        target != null && NoellesRoleStateComponent.KEY.get(target).isTaotieSwallowed(),
                        sameWorld,
                        target != null && taotie.squaredDistanceTo(target) <= TaotieSwallowPolicy.SWALLOW_DISTANCE_SQUARED,
                        target != null && taotie.canSee(target)
                )
        );
        if (result.blocked()) {
            sendBlockedMessage(taotie, taotieState, currentGameTime, result);
            return;
        }

        swallow(taotie, target, game, currentGameTime);
    }

    private static void swallow(
            ServerPlayerEntity taotie,
            ServerPlayerEntity target,
            GameWorldComponent game,
            long currentGameTime
    ) {
        NoellesRoleStateComponent taotieState = NoellesRoleStateComponent.KEY.get(taotie);
        NoellesRoleStateComponent targetState = NoellesRoleStateComponent.KEY.get(target);
        UUID taotieUuid = taotie.getUuid();
        UUID targetUuid = target.getUuid();

        taotieState.trackTaotieSwallowedPlayer(targetUuid);
        targetState.setTaotieSwallowedBy(taotieUuid);
        SWALLOWED_BY_TARGET.put(targetUuid, taotieUuid);
        TARGETS_BY_TAOTIE.computeIfAbsent(taotieUuid, ignored -> new HashSet<>()).add(targetUuid);
        taotieState.tryBeginAbilityCooldown(
                TaotieSwallowPolicy.ABILITY_ID,
                currentGameTime,
                TaotieSwallowPolicy.calculatedSwallowCooldownTicks(game.getRoles().size())
        );
        applyTemporarySpectatorCamera(taotie, target);
        taotie.sendMessage(Text.translatable(
                "message.strawcraft.taotie.swallowed",
                target.getDisplayName()
        ).formatted(Formatting.GREEN), true);
    }

    private static void applyTemporarySpectatorCamera(ServerPlayerEntity taotie, ServerPlayerEntity target) {
        // Swallowing is not a Wathe death: StrawCraft only applies a reversible spectator camera state.
        // 吞噬不是 Wathe 死亡：StrawCraft 只施加可逆的临时旁观镜头状态。
        target.changeGameMode(GameMode.SPECTATOR);
        target.setCameraEntity(taotie);
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        if (SWALLOWED_BY_TARGET.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, UUID> entry : List.copyOf(SWALLOWED_BY_TARGET.entrySet())) {
            ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            ServerPlayerEntity taotie = world.getServer().getPlayerManager().getPlayer(entry.getValue());
            if (target == null) {
                releaseOfflineTarget(world.getServer(), entry.getKey(), entry.getValue());
                continue;
            }
            if (target.getServerWorld() != world) {
                continue;
            }
            if (!shouldRemainSwallowed(world, taotie, target)) {
                releaseSwallowedTarget(world.getServer(), entry.getKey());
                continue;
            }
            applyTemporarySpectatorCamera(taotie, target);
        }
    }

    private static boolean shouldRemainSwallowed(ServerWorld world, @Nullable ServerPlayerEntity taotie, ServerPlayerEntity target) {
        if (taotie == null || taotie.getServerWorld() != world) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        return game.isRunning()
                && isTaotieRole(game.getRole(taotie))
                && GameFunctions.isPlayerAliveAndSurvival(taotie)
                && target.isAlive()
                && game.getRole(target) != null
                && NoellesRoleStateComponent.KEY.get(target).taotieSwallowedBy().filter(taotie.getUuid()::equals).isPresent();
    }

    private static void handleRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        UUID victimUuid = context.official().victimUuid();
        if (context.victimRoleId().filter(TaotieSwallowPolicy.TAOTIE_ROLE::equals).isPresent()) {
            releaseAllSwallowedBy(context.world().getServer(), victimUuid);
        }
        releaseSwallowedTarget(context.world().getServer(), victimUuid);
    }

    private static void handleRoleAssigned(PlayerEntity player, Role role) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        releaseSwallowedTarget(serverPlayer.getServer(), serverPlayer.getUuid());
        if (!isTaotieRole(role)) {
            releaseAllSwallowedBy(serverPlayer.getServer(), serverPlayer.getUuid());
        }
    }

    private static void releaseDisconnected(ServerPlayerEntity player, MinecraftServer server) {
        releaseSwallowedTarget(server, player.getUuid(), player);
        releaseAllSwallowedBy(server, player.getUuid(), player);
    }

    private static void releaseAllInWorld(ServerWorld world) {
        for (Map.Entry<UUID, UUID> entry : List.copyOf(SWALLOWED_BY_TARGET.entrySet())) {
            ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (target == null || target.getServerWorld() == world) {
                releaseSwallowedTarget(world.getServer(), entry.getKey());
            }
        }
    }

    static void releaseSwallowedTarget(MinecraftServer server, UUID targetUuid) {
        releaseSwallowedTarget(server, targetUuid, null);
    }

    private static void releaseSwallowedTarget(
            MinecraftServer server,
            UUID targetUuid,
            @Nullable ServerPlayerEntity knownTarget
    ) {
        UUID taotieUuid = SWALLOWED_BY_TARGET.remove(targetUuid);
        if (taotieUuid == null) {
            return;
        }
        clearOwnerTarget(server, taotieUuid, targetUuid);
        ServerPlayerEntity target = knownTarget != null ? knownTarget : server.getPlayerManager().getPlayer(targetUuid);
        if (target == null) {
            return;
        }
        NoellesRoleStateComponent.KEY.get(target).clearTaotieSwallowedBy();
        restoreTemporarySpectatorCamera(target);
    }

    private static void releaseOfflineTarget(MinecraftServer server, UUID targetUuid, UUID taotieUuid) {
        SWALLOWED_BY_TARGET.remove(targetUuid);
        clearOwnerTarget(server, taotieUuid, targetUuid);
    }

    private static void releaseAllSwallowedBy(MinecraftServer server, UUID taotieUuid) {
        releaseAllSwallowedBy(server, taotieUuid, null);
    }

    private static void releaseAllSwallowedBy(
            MinecraftServer server,
            UUID taotieUuid,
            @Nullable ServerPlayerEntity knownTaotie
    ) {
        Set<UUID> targets = TARGETS_BY_TAOTIE.getOrDefault(taotieUuid, Set.of());
        for (UUID targetUuid : List.copyOf(targets)) {
            releaseSwallowedTarget(server, targetUuid);
        }
        TARGETS_BY_TAOTIE.remove(taotieUuid);
        ServerPlayerEntity taotie = knownTaotie != null ? knownTaotie : server.getPlayerManager().getPlayer(taotieUuid);
        if (taotie != null) {
            NoellesRoleStateComponent.KEY.get(taotie).clearTaotieSwallowedPlayers();
        }
    }

    private static void clearOwnerTarget(MinecraftServer server, UUID taotieUuid, UUID targetUuid) {
        Set<UUID> targets = TARGETS_BY_TAOTIE.get(taotieUuid);
        if (targets != null) {
            targets.remove(targetUuid);
            if (targets.isEmpty()) {
                TARGETS_BY_TAOTIE.remove(taotieUuid);
            }
        }
        ServerPlayerEntity taotie = server.getPlayerManager().getPlayer(taotieUuid);
        if (taotie != null) {
            NoellesRoleStateComponent.KEY.get(taotie).untrackTaotieSwallowedPlayer(targetUuid);
        }
    }

    private static void restoreTemporarySpectatorCamera(ServerPlayerEntity target) {
        target.setCameraEntity(target);
        if (target.isAlive() && !target.isCreative()) {
            target.changeGameMode(GameMode.SURVIVAL);
        }
    }

    private static void collectWinContributions(
            StrawWinEvents.WinContext context,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        if (context.world().isEmpty()) {
            return;
        }
        ServerWorld world = context.world().orElseThrow();
        for (StrawWinEvents.Participant participant : context.participants()) {
            if (!participant.alive() || participant.roleId().filter(TaotieSwallowPolicy.TAOTIE_ROLE::equals).isEmpty()) {
                continue;
            }
            PlayerEntity player = world.getPlayerByUuid(participant.playerUuid());
            if (player == null) {
                continue;
            }
            NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(player);
            collectWinContribution(
                    participant.playerUuid(),
                    state.taotieSwallowedPlayers(),
                    context.participants(),
                    contribution
            );
        }
    }

    static void collectWinContribution(
            UUID taotieUuid,
            Set<UUID> swallowedPlayers,
            List<StrawWinEvents.Participant> participants,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        if (TaotieSwallowPolicy.hasSwallowedEveryone(
                taotieUuid,
                new TaotieSwallowPolicy.RoundState(
                        swallowedPlayers,
                        0,
                        false,
                        0,
                        TaotieSwallowPolicy.triggerThreshold(participants.size()),
                        participants.size(),
                        TaotieSwallowPolicy.calculatedSwallowCooldownTicks(participants.size())
                ),
                participantStatuses(taotieUuid, swallowedPlayers, participants)
        )) {
            contribution
                    .replaceDefaultWin(StrawWinEvents.DefaultWin.LOOSE_END)
                    .addExtraWinner(taotieUuid, TaotieSwallowPolicy.TAOTIE_ROLE, SWALLOWED_ALL_TRIGGER);
        }
    }

    private static List<TaotieSwallowPolicy.ParticipantStatus> participantStatuses(
            UUID taotieUuid,
            Set<UUID> swallowedPlayers,
            List<StrawWinEvents.Participant> participants
    ) {
        List<TaotieSwallowPolicy.ParticipantStatus> statuses = new ArrayList<>();
        for (StrawWinEvents.Participant participant : participants) {
            boolean eligible = participant.assigned()
                    && (participant.alive() || swallowedPlayers.contains(participant.playerUuid()));
            statuses.add(new TaotieSwallowPolicy.ParticipantStatus(participant.playerUuid(), eligible));
        }
        return statuses;
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity taotie,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            TaotieSwallowPolicy.ValidationResult result
    ) {
        switch (result) {
            case COOLDOWN -> taotie.sendMessage(Text.translatable(
                    "message.strawcraft.taotie.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case DIFFERENT_WORLD -> taotie.sendMessage(
                    Text.translatable("message.strawcraft.taotie.wrong_dimension").formatted(Formatting.RED), true);
            case TOO_FAR, NO_LINE_OF_SIGHT -> taotie.sendMessage(
                    Text.translatable("message.strawcraft.taotie.out_of_reach").formatted(Formatting.RED), true);
            case INVALID_TARGET, TARGET_NOT_ACTIVE -> taotie.sendMessage(
                    Text.translatable("message.strawcraft.taotie.invalid_target").formatted(Formatting.RED), true);
            default -> {
            }
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(TaotieSwallowPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isTaotieRole(@Nullable Role role) {
        return StrawRoleMeaning.receivesTaotieSwallow(role);
    }
}
