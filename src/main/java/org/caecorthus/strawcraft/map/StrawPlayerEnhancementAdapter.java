package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.StrawCraft;

import java.util.Optional;

public final class StrawPlayerEnhancementAdapter {
    private static final Identifier GRAVITY_MODIFIER_ID = StrawCraft.id("map_gravity_modifier");

    private StrawPlayerEnhancementAdapter() {
    }

    public static void applyInitializedPlayers(ServerWorld world, GameWorldComponent gameComponent) {
        Optional<StrawMapEnhancements> enhancements = enhancementsFor(world, gameComponent);
        if (enhancements.isEmpty()) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (gameComponent.getRoles().containsKey(player.getUuid())) {
                applyGravity(player, enhancements.get().gravity().gravityMultiplier());
            }
        }
    }

    public static void clearPlayers(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            applyGravity(player, 1.0F);
        }
    }

    public static float movementSpeed(PlayerEntity player, float vanillaSpeed) {
        Optional<StrawMapEnhancements> enhancements = activeEnhancements(player);
        if (enhancements.isEmpty()) {
            return vanillaSpeed;
        }

        // This is a per-map multiplier layered on top of vanilla speed, not a return to Wathe stamina limits.
        // 这是地图配置显式启用后的速度倍率，不是恢复 Wathe 的体力/心情移动限制。
        float multiplier = player.isSprinting()
                ? enhancements.get().movement().sprintSpeedMultiplier()
                : enhancements.get().movement().walkSpeedMultiplier();
        return vanillaSpeed * multiplier;
    }

    public static boolean allowsJump(PlayerEntity player) {
        return activeEnhancements(player)
                .map(enhancements -> enhancements.jump().allowed())
                .orElse(true);
    }

    private static Optional<StrawMapEnhancements> activeEnhancements(PlayerEntity player) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameComponent == null || !gameComponent.isRunning() || !gameComponent.getRoles().containsKey(player.getUuid())) {
            return Optional.empty();
        }
        return enhancementsFor(player.getWorld().getRegistryKey().getValue(), gameComponent)
                .filter(StrawMapEnhancements::hasServerRuntimeEnhancements);
    }

    private static Optional<StrawMapEnhancements> enhancementsFor(ServerWorld world, GameWorldComponent gameComponent) {
        return enhancementsFor(world.getRegistryKey().getValue(), gameComponent);
    }

    private static Optional<StrawMapEnhancements> enhancementsFor(Identifier dimensionId, GameWorldComponent gameComponent) {
        return StrawCurrentMapResolver.resolve(dimensionId, gameComponent)
                .map(StrawMapEntry::enhancements)
                .filter(StrawMapEnhancements::hasServerRuntimeEnhancements);
    }

    static Optional<StrawMapEnhancements> enhancementsFor(
            Identifier dimensionId,
            Identifier gameModeId,
            Identifier mapEffectId
    ) {
        return StrawCurrentMapResolver.resolve(dimensionId, gameModeId, mapEffectId)
                .map(StrawMapEntry::enhancements)
                .filter(StrawMapEnhancements::hasServerRuntimeEnhancements);
    }

    private static void applyGravity(ServerPlayerEntity player, float multiplier) {
        EntityAttributeInstance gravity = player.getAttributeInstance(EntityAttributes.GENERIC_GRAVITY);
        if (gravity == null) {
            return;
        }
        if (gravity.hasModifier(GRAVITY_MODIFIER_ID)) {
            gravity.removeModifier(GRAVITY_MODIFIER_ID);
        }
        if (multiplier != 1.0F) {
            // Explicit per-map gravity uses the official vanilla 1.21.1 attribute and stays addon-safe.
            // 显式地图重力配置使用 1.21.1 官方原版属性，因此不需要改 Wathe 或依赖 Spark 组件。
            gravity.addTemporaryModifier(new EntityAttributeModifier(
                    GRAVITY_MODIFIER_ID,
                    multiplier - 1.0F,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}
