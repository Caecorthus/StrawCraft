package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DetectiveKillHistoryRuntime {
    public static final int INVESTIGATION_COOLDOWN_TICKS = 90 * 20;
    public static final String DETECTIVE_INVESTIGATE_COOLDOWN = "detective_investigate";

    private static final Identifier LEGACY_WORLD_ID = StrawDeathEvents.UNKNOWN_WORLD;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<Identifier, DetectiveKillHistory> HISTORIES_BY_WORLD = new HashMap<>();

    private DetectiveKillHistoryRuntime() {
    }

    public static void registerEvents() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(DetectiveInvestigationPayload.ID, DetectiveInvestigationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DetectiveInvestigationPayload.ID, DetectiveKillHistoryRuntime::handleInvestigation);

        // Spark NoellesRoles records KillPlayer.AFTER; StrawCraft waits for its official-death completion seam.
        // Spark 版 NoellesRoles 在 KillPlayer.AFTER 记录；StrawCraft 等自己的官方死亡完成 seam 再记录。
        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.register(DetectiveKillHistoryRuntime::recordOfficialDeath);
    }

    public static void recordOfficialDeath(StrawDeathEvents.OfficialDeathContext context) {
        context.killerUuid().ifPresent(killerUuid ->
                recordKill(context.worldId(), killerUuid, context.victimUuid(), context.deathReason(), context.gameTime()));
    }

    public static void recordKill(
            @Nullable UUID killerUuid,
            @Nullable UUID victimUuid,
            @Nullable Identifier deathReason,
            long gameTime
    ) {
        recordKill(LEGACY_WORLD_ID, killerUuid, victimUuid, deathReason, gameTime);
    }

    public static void recordKill(
            Identifier worldId,
            @Nullable UUID killerUuid,
            @Nullable UUID victimUuid,
            @Nullable Identifier deathReason,
            long gameTime
    ) {
        synchronized (DetectiveKillHistoryRuntime.class) {
            DetectiveKillHistory history = historyFor(worldId);
            history.recordKill(killerUuid, victimUuid, deathReason, gameTime);
            history.expireOldKills(gameTime);
        }
    }

    public static DetectiveInvestigationPolicy.Result investigate(@Nullable UUID targetUuid, long currentGameTime) {
        return investigate(LEGACY_WORLD_ID, targetUuid, currentGameTime);
    }

    public static DetectiveInvestigationPolicy.Result investigate(
            Identifier worldId,
            @Nullable UUID targetUuid,
            long currentGameTime
    ) {
        synchronized (DetectiveKillHistoryRuntime.class) {
            DetectiveKillHistory history = historyFor(worldId);
            history.expireOldKills(currentGameTime);
            return DetectiveInvestigationPolicy.investigate(targetUuid, history, currentGameTime);
        }
    }

    public static CooldownAttempt tryBeginInvestigationCooldown(NoellesRoleState state, long currentGameTime) {
        return state.tryBeginAbilityCooldown(
                DETECTIVE_INVESTIGATE_COOLDOWN,
                currentGameTime,
                INVESTIGATION_COOLDOWN_TICKS
        ) ? CooldownAttempt.ALLOWED : CooldownAttempt.COOLDOWN;
    }

    public static CooldownAttempt tryBeginInvestigationCooldown(
            NoellesRoleStateComponent state,
            long currentGameTime
    ) {
        return state.tryBeginAbilityCooldown(
                DETECTIVE_INVESTIGATE_COOLDOWN,
                currentGameTime,
                INVESTIGATION_COOLDOWN_TICKS
        ) ? CooldownAttempt.ALLOWED : CooldownAttempt.COOLDOWN;
    }

    public static void resetAll() {
        synchronized (DetectiveKillHistoryRuntime.class) {
            HISTORIES_BY_WORLD.values().forEach(DetectiveKillHistory::reset);
            HISTORIES_BY_WORLD.clear();
        }
    }

    private static void handleInvestigation(DetectiveInvestigationPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity detective = context.player();
        Identifier worldId = detective.getWorld().getRegistryKey().getValue();
        long currentGameTime = detective.getWorld().getTime();

        GameWorldComponent game = GameWorldComponent.KEY.get(detective.getWorld());
        if (!canInvestigateAsDetective(detective, game)) {
            return;
        }

        PlayerEntity target = detective.getWorld().getPlayerByUuid(payload.targetPlayer());
        if (!isInteractionAllowed(detective, target, game, currentGameTime)) {
            return;
        }

        DetectiveInvestigationPolicy.Result result = investigate(worldId, target.getUuid(), currentGameTime);
        detective.sendMessage(resultMessage(result, target), true);
    }

    private static boolean canInvestigateAsDetective(ServerPlayerEntity detective, GameWorldComponent game) {
        Role role = game.getRole(detective);
        return game.isRunning()
                && detectiveRole(role)
                && GameFunctions.isPlayerAliveAndSurvival(detective);
    }

    private static boolean isInteractionAllowed(
            ServerPlayerEntity detective,
            @Nullable PlayerEntity target,
            GameWorldComponent game,
            long currentGameTime
    ) {
        // Detective uses the shared Noelles role-state CCA; kill history remains world-scoped above.
        // 侦探冷却使用共享 Noelles 职业状态 CCA；击杀历史仍保留在上面的世界维度记录里。
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(detective);
        boolean cooldownReady = !roleState.isAbilityOnCooldown(DETECTIVE_INVESTIGATE_COOLDOWN, currentGameTime);
        DetectiveInvestigationPolicy.ValidationResult validation = DetectiveInvestigationPolicy.validateInteraction(
                new DetectiveInvestigationPolicy.InteractionInput(
                        game.isRunning(),
                        detectiveRole(game.getRole(detective)),
                        GameFunctions.isPlayerAliveAndSurvival(detective),
                        target != null,
                        target == detective,
                        target != null && game.getRole(target) != null,
                        target != null && GameFunctions.isPlayerAliveAndSurvival(target),
                        target == null ? Double.POSITIVE_INFINITY : detective.squaredDistanceTo(target),
                        target != null && detective.canSee(target),
                        cooldownReady
                )
        );
        if (validation.blocked()) {
            return false;
        }
        return tryBeginInvestigationCooldown(roleState, currentGameTime) == CooldownAttempt.ALLOWED;
    }

    private static Text resultMessage(DetectiveInvestigationPolicy.Result result, PlayerEntity target) {
        if (result == DetectiveInvestigationPolicy.Result.SUSPICIOUS) {
            return Text.translatable("message.strawcraft.detective.suspicious", target.getDisplayName())
                    .formatted(Formatting.RED);
        }
        return Text.translatable("message.strawcraft.detective.clear", target.getDisplayName())
                .formatted(Formatting.GREEN);
    }

    private static DetectiveKillHistory historyFor(Identifier worldId) {
        return HISTORIES_BY_WORLD.computeIfAbsent(worldId, ignored -> new DetectiveKillHistory());
    }

    private static boolean detectiveRole(@Nullable Role role) {
        return StrawRoleMeaning.receivesDetectiveInvestigation(role);
    }

    public enum CooldownAttempt {
        ALLOWED,
        COOLDOWN
    }

}
