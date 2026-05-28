package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MermaidWaterAdaptationRuntime {
    static final Identifier MAX_SPRINT_TIME_MODIFIER_ID =
            StrawCraft.id("mermaid_water_adaptation_max_sprint_time");

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final String WATHE_ATTRIBUTES_CLASS = "dev.doctor4t.wathe.WatheAttributes";
    private static final String WATHE_MAX_SPRINT_TIME_FIELD = "MAX_SPRINT_TIME";
    private static Optional<RegistryEntry<EntityAttribute>> maxSprintTimeAttribute;

    private MermaidWaterAdaptationRuntime() {
    }

    public static void registerEvents() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(MermaidWaterAdaptationRuntime::tickServer);
        GameEvents.ON_FINISH_INITIALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                clearWorld(serverWorld);
            }
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                clearWorld(serverWorld);
            }
        });
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!game.isRunning()) {
            clearWorld(world);
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            tickPlayer(game, player);
        }
    }

    private static void tickPlayer(GameWorldComponent game, ServerPlayerEntity player) {
        MermaidWaterAdaptationPolicy.Result result = MermaidWaterAdaptationPolicy.evaluate(
                new MermaidWaterAdaptationPolicy.Input(
                        game.isRunning(),
                        isMermaidRole(game.getRole(player)),
                        GameFunctions.isPlayerAliveAndSurvival(player),
                        isTouchingOrSubmergedInWater(player)
                )
        );

        if (result == MermaidWaterAdaptationPolicy.Result.ACTIVE) {
            applyPlayer(player);
            return;
        }

        clearPlayer(player);
    }

    private static void applyPlayer(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.DOLPHINS_GRACE,
                MermaidWaterAdaptationPolicy.EFFECT_DURATION_TICKS,
                0,
                true,
                false,
                true
        ));
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                MermaidWaterAdaptationPolicy.EFFECT_DURATION_TICKS,
                0,
                true,
                false,
                true
        ));
        applySprintModifier(player);
    }

    private static void applySprintModifier(ServerPlayerEntity player) {
        officialMaxSprintTimeAttribute().ifPresent(attribute -> {
            EntityAttributeInstance maxSprintTime = player.getAttributeInstance(attribute);
            if (maxSprintTime == null || maxSprintTime.hasModifier(MAX_SPRINT_TIME_MODIFIER_ID)) {
                return;
            }

            // WatheAttributes.MAX_SPRINT_TIME stays official-owned; StrawCraft only lends a round-scoped bonus.
            // WatheAttributes.MAX_SPRINT_TIME 仍归官方所有；StrawCraft 只临时借出本回合加成。
            // WatheAttributes.MAX_SPRINT_TIME 仍归官方 Wathe 所有；StrawCraft 只提供回合内临时加成。
            // WatheAttributes.MAX_SPRINT_TIME 仍归官方 Wathe 所有；本附属模组只提供回合内临时加成。
            maxSprintTime.addTemporaryModifier(new EntityAttributeModifier(
                    MAX_SPRINT_TIME_MODIFIER_ID,
                    MermaidWaterAdaptationPolicy.MAX_SPRINT_TIME_BONUS_TICKS,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        });
    }

    static void clearWorld(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            clearPlayer(player);
        }
    }

    private static void clearPlayer(ServerPlayerEntity player) {
        officialMaxSprintTimeAttribute().ifPresent(attribute -> {
            EntityAttributeInstance maxSprintTime = player.getAttributeInstance(attribute);
            if (maxSprintTime != null && maxSprintTime.hasModifier(MAX_SPRINT_TIME_MODIFIER_ID)) {
                maxSprintTime.removeModifier(MAX_SPRINT_TIME_MODIFIER_ID);
            }
        });
    }

    private static boolean isMermaidRole(Role role) {
        return StrawRoleMeaning.receivesMermaidWaterAdaptation(role);
    }

    private static boolean isTouchingOrSubmergedInWater(ServerPlayerEntity player) {
        return player.isTouchingWater() || player.isSubmergedInWater();
    }

    @SuppressWarnings("unchecked")
    private static Optional<RegistryEntry<EntityAttribute>> officialMaxSprintTimeAttribute() {
        if (maxSprintTimeAttribute != null) {
            return maxSprintTimeAttribute;
        }

        try {
            Class<?> watheAttributes = Class.forName(WATHE_ATTRIBUTES_CLASS);
            Field maxSprintTime = watheAttributes.getField(WATHE_MAX_SPRINT_TIME_FIELD);
            Object value = maxSprintTime.get(null);
            if (value instanceof RegistryEntry<?> entry && entry.value() instanceof EntityAttribute) {
                maxSprintTimeAttribute = Optional.of((RegistryEntry<EntityAttribute>) entry);
                return maxSprintTimeAttribute;
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException ignored) {
            // Older Wathe jars keep sprint time on Role; cleanup stays harmless until the official attribute exists.
            // 旧版 Wathe 仍把冲刺时间放在 Role 上；官方属性存在前清理逻辑保持无害。
            // 旧版 Wathe 将冲刺时间放在 Role 上；在官方属性出现前，这里的清理保持无害。
            // 旧版 Wathe 将冲刺时间放在 Role 上；在官方属性出现前，这里的清理保持无害。
        }

        maxSprintTimeAttribute = Optional.empty();
        return maxSprintTimeAttribute;
    }
}
