package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MorphlingDisguiseRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private MorphlingDisguiseRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(MorphlingDisguisePayload.ID, MorphlingDisguisePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MorphlingCorpseTogglePayload.ID, MorphlingCorpseTogglePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MorphlingDisguisePayload.ID, MorphlingDisguiseRuntime::handleMorph);
        ServerPlayNetworking.registerGlobalReceiver(MorphlingCorpseTogglePayload.ID, MorphlingDisguiseRuntime::handleCorpseToggle);
        ServerTickEvents.END_SERVER_TICK.register(MorphlingDisguiseRuntime::tickServer);
    }

    private static void handleMorph(MorphlingDisguisePayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity morphling = context.player();
        ServerWorld world = morphling.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(morphling);
        ServerPlayerEntity target = morphling.getServer().getPlayerManager().getPlayer(payload.target());
        boolean sameWorld = target != null && target.getServerWorld() == world;

        // Clients provide only a target UUID; active role, target state, timers, and corpse intent stay server-owned.
        // 客户端只提供目标 UUID；职业、目标状态、计时器和尸体模式意图都由服务端掌控。
        MorphlingDisguisePolicy.ValidationResult result = MorphlingDisguisePolicy.validateStart(
                new MorphlingDisguisePolicy.StartInput(
                        game.isRunning(),
                        isMorphlingRole(game.getRole(morphling)),
                        GameFunctions.isPlayerAliveAndSurvival(morphling),
                        isSwallowed(morphling),
                        target != null,
                        target == morphling,
                        target != null && game.getRole(target) != null,
                        target != null && GameFunctions.isPlayerAliveAndSurvival(target),
                        target != null && isSwallowed(target),
                        sameWorld,
                        !roleState.isAbilityOnCooldown(MorphlingDisguisePolicy.ABILITY_ID, currentGameTime),
                        roleState.morphlingDisguiseState().morphTicks() == 0
                )
        );
        if (result.blocked()) {
            return;
        }

        roleState.setMorphlingDisguiseState(MorphlingDisguisePolicy.startMorph(
                roleState.morphlingDisguiseState(),
                target.getUuid(),
                currentGameTime
        ));
    }

    private static void handleCorpseToggle(
            MorphlingCorpseTogglePayload payload,
            ServerPlayNetworking.Context context
    ) {
        ServerPlayerEntity morphling = context.player();
        ServerWorld world = morphling.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!game.isRunning()
                || !isMorphlingRole(game.getRole(morphling))
                || !GameFunctions.isPlayerAliveAndSurvival(morphling)
                || isSwallowed(morphling)) {
            return;
        }

        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(morphling);
        roleState.setMorphlingDisguiseState(MorphlingDisguisePolicy.toggleCorpseMode(
                roleState.morphlingDisguiseState()
        ));
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerPlayerEntity morphling : server.getPlayerManager().getPlayerList()) {
            tickMorphling(morphling);
        }
    }

    private static void tickMorphling(ServerPlayerEntity morphling) {
        ServerWorld world = morphling.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!isMorphlingRole(game.getRole(morphling))) {
            return;
        }

        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(morphling);
        NoellesRoleState.MorphlingDisguiseState current = roleState.morphlingDisguiseState();
        if (!current.hasState()) {
            return;
        }
        if (!game.isRunning()) {
            roleState.clearMorphlingDisguiseState();
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(morphling)) {
            if (current.morphTicks() > 0) {
                roleState.setAbilityCooldown(MorphlingDisguisePolicy.ABILITY_ID, world.getTime(), MorphlingDisguisePolicy.RECOVERY_TICKS);
                roleState.setMorphlingDisguiseState(MorphlingDisguisePolicy.stopMorph(current));
            }
            return;
        }
        if (current.corpseMode()) {
            applyCorpseModeEffect(morphling);
        }

        boolean targetStillValid = current.disguiseUuid()
                .filter(targetUuid -> disguiseTargetStillValid(world, game, morphling, targetUuid))
                .isPresent();
        NoellesRoleState.MorphlingDisguiseState next = MorphlingDisguisePolicy.tick(current, targetStillValid);
        if (current.equals(next)) {
            return;
        }
        if (current.morphTicks() > 0 && next.morphTicks() < 0) {
            roleState.setAbilityCooldown(MorphlingDisguisePolicy.ABILITY_ID, world.getTime(), MorphlingDisguisePolicy.RECOVERY_TICKS);
        }
        roleState.setMorphlingDisguiseState(next);
    }

    private static void applyCorpseModeEffect(ServerPlayerEntity morphling) {
        // Corpse mode is a visual fake-death stance; movement penalty is reapplied server-side while active.
        // 尸体模式只是伪装死亡姿态；移动惩罚在启用期间持续由服务端补发。
        morphling.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 4, 2, true, false, false));
    }

    private static boolean disguiseTargetStillValid(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity morphling,
            UUID targetUuid
    ) {
        ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(targetUuid);
        return target != null
                && target != morphling
                && target.getServerWorld() == world
                && game.getRole(target) != null
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !isSwallowed(target);
    }

    private static boolean isMorphlingRole(@Nullable Role role) {
        return StrawRoleMeaning.receivesMorphlingDisguise(role);
    }

    private static boolean isSwallowed(ServerPlayerEntity player) {
        // Taotie foundation exposes only server-owned swallowed state; no spectator or camera behavior is implied here.
        // 饕餮基础层这里只提供服务端吞噬状态；不代表已经实现旁观者或镜头行为。
        // 饕餮吞噬目前还没有 StrawCraft 运行时谓词；先保留这个源逻辑守卫位置，方便之后接入。
        return NoellesRoleStateComponent.KEY.get(player).isTaotieSwallowed();
    }
}
