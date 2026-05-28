package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypeFilter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VultureBodyFeastRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private VultureBodyFeastRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(VultureFeastPayload.ID, VultureFeastPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VultureFeastPayload.ID, VultureBodyFeastRuntime::handleFeast);
    }

    public static ValidationResult validateInteraction(InteractionInput input) {
        if (!input.gameRunning()) {
            return ValidationResult.NOT_RUNNING;
        }
        if (!input.vultureRole()) {
            return ValidationResult.NOT_VULTURE;
        }
        if (!input.playerAlive()) {
            return ValidationResult.NOT_ALIVE;
        }
        if (!input.cooldownReady()) {
            return ValidationResult.COOLDOWN;
        }
        if (!input.bodyFound()) {
            return ValidationResult.NO_BODY;
        }
        if (input.nearestBodyDistanceSquared() > VultureBodyFeastPolicy.FEAST_RANGE_SQUARED) {
            return ValidationResult.OUT_OF_RANGE;
        }
        return ValidationResult.ALLOWED;
    }

    private static void handleFeast(VultureFeastPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity vulture = context.player();
        ServerWorld world = vulture.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(vulture);
        Optional<PlayerBodyEntity> nearestBody = nearestBody(vulture);

        // The client sends intent only; the server owns all gameplay checks before mutating the corpse.
        // 客户端只发送意图；服务端在修改尸体前负责所有玩法校验。
        ValidationResult validation = validateInteraction(new InteractionInput(
                game.isRunning(),
                isVultureRole(game.getRole(vulture)),
                GameFunctions.isPlayerAliveAndSurvival(vulture),
                !roleState.isAbilityOnCooldown(VultureBodyFeastPolicy.VULTURE_EAT_COOLDOWN, currentGameTime),
                nearestBody.isPresent(),
                nearestBody.map(body -> body.squaredDistanceTo(vulture)).orElse(Double.POSITIVE_INFINITY)
        ));
        if (validation != ValidationResult.ALLOWED) {
            return;
        }

        PlayerBodyEntity body = nearestBody.orElseThrow();
        VultureBodyFeastPolicy.FeastResult result =
                VultureBodyFeastPolicy.recordSuccessfulFeast(roleState, body.getUuid(), currentGameTime);
        FeastRuntimeEffects effects = planRuntimeEffects(validation, result);
        if (effects.skipAll()) {
            return;
        }

        if (effects.beginCooldown()) {
            roleState.tryBeginAbilityCooldown(
                    VultureBodyFeastPolicy.VULTURE_EAT_COOLDOWN,
                    currentGameTime,
                    VultureBodyFeastPolicy.FEAST_COOLDOWN_TICKS
            );
        }
        if (effects.consumeBody()) {
            spawnFeastEffects(world, body);
            vulture.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 10 * 20, 2, false, false, true));
            ScavengerHiddenBodies.clearConsumedBody(world, body);
            body.discard();
        }
        if (effects.sendProgressMessage()) {
            vulture.sendMessage(progressMessage(result), true);
        }
    }

    public static FeastRuntimeEffects planRuntimeEffects(
            ValidationResult validation,
            VultureBodyFeastPolicy.FeastResult result
    ) {
        if (validation != ValidationResult.ALLOWED || !result.accepted()) {
            return FeastRuntimeEffects.NONE;
        }
        return FeastRuntimeEffects.ACCEPTED_FEAST;
    }

    private static Optional<PlayerBodyEntity> nearestBody(ServerPlayerEntity vulture) {
        return vulture.getServerWorld()
                .getEntitiesByType(
                        TypeFilter.equals(PlayerBodyEntity.class),
                        vulture.getBoundingBox().expand(VultureBodyFeastPolicy.FEAST_RANGE),
                        body -> body.squaredDistanceTo(vulture) <= VultureBodyFeastPolicy.FEAST_RANGE_SQUARED
                )
                .stream()
                .min(Comparator.comparingDouble(body -> body.squaredDistanceTo(vulture)));
    }

    private static boolean isVultureRole(@Nullable Role role) {
        return StrawRoleMeaning.receivesVultureBodyFeast(role);
    }

    private static void spawnFeastEffects(ServerWorld world, PlayerBodyEntity body) {
        world.spawnParticles(ParticleTypes.SMOKE, body.getX(), body.getY() + 0.5D, body.getZ(), 30, 0.3D, 0.3D, 0.3D, 0.02D);
        world.spawnParticles(ParticleTypes.SOUL, body.getX(), body.getY() + 0.5D, body.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.01D);
    }

    private static Text progressMessage(VultureBodyFeastPolicy.FeastResult result) {
        if (result.won()) {
            return Text.translatable("message.strawcraft.vulture.claimed_win").formatted(Formatting.GREEN);
        }
        return Text.translatable(
                "message.strawcraft.vulture.progress",
                result.bodiesEaten(),
                result.bodiesRequired()
        ).formatted(Formatting.YELLOW);
    }

    public record InteractionInput(
            boolean gameRunning,
            boolean vultureRole,
            boolean playerAlive,
            boolean cooldownReady,
            boolean bodyFound,
            double nearestBodyDistanceSquared
    ) {
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_RUNNING,
        NOT_VULTURE,
        NOT_ALIVE,
        COOLDOWN,
        NO_BODY,
        OUT_OF_RANGE
    }

    public record FeastRuntimeEffects(
            boolean beginCooldown,
            boolean consumeBody,
            boolean sendProgressMessage
    ) {
        private static final FeastRuntimeEffects NONE = new FeastRuntimeEffects(false, false, false);
        private static final FeastRuntimeEffects ACCEPTED_FEAST = new FeastRuntimeEffects(true, true, true);

        public boolean skipAll() {
            return !beginCooldown && !consumeBody && !sendProgressMessage;
        }
    }
}
