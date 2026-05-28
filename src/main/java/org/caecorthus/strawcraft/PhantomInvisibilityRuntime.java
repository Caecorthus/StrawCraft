package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PhantomInvisibilityRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private PhantomInvisibilityRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(PhantomInvisibilityPayload.ID, PhantomInvisibilityPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PhantomInvisibilityPayload.ID, PhantomInvisibilityRuntime::handleInvisibility);
    }

    private static void handleInvisibility(PhantomInvisibilityPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity phantom = context.player();
        ServerWorld world = phantom.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(phantom);

        // The client sends empty intent; role, round, life state, effect, and cooldown stay server-owned.
        // 客户端只发送空意图；身份、回合、生存状态、效果和冷却都由服务端裁决。
        PhantomInvisibilityPolicy.Result result = PhantomInvisibilityPolicy.validate(new PhantomInvisibilityPolicy.Input(
                game.isRunning(),
                isPhantomRole(game.getRole(phantom)),
                GameFunctions.isPlayerAliveAndSurvival(phantom),
                !roleState.isAbilityOnCooldown(PhantomInvisibilityPolicy.ABILITY_ID, currentGameTime)
        ));
        if (result.blocked()) {
            sendBlockedMessage(phantom, roleState, currentGameTime, result);
            return;
        }

        phantom.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY,
                PhantomInvisibilityPolicy.INVISIBILITY_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
        roleState.tryBeginAbilityCooldown(
                PhantomInvisibilityPolicy.ABILITY_ID,
                currentGameTime,
                PhantomInvisibilityPolicy.COOLDOWN_TICKS
        );
        phantom.sendMessage(Text.translatable("message.strawcraft.phantom.invisible")
                .formatted(Formatting.AQUA), true);
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity phantom,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            PhantomInvisibilityPolicy.Result result
    ) {
        if (result == PhantomInvisibilityPolicy.Result.COOLDOWN) {
            phantom.sendMessage(Text.translatable(
                    "message.strawcraft.phantom.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(PhantomInvisibilityPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isPhantomRole(Role role) {
        return StrawRoleMeaning.receivesPhantomInvisibility(role);
    }
}
