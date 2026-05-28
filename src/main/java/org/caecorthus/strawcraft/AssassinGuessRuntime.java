package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AssassinGuessRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private AssassinGuessRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(AssassinGuessPayload.ID, AssassinGuessPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AssassinGuessPayload.ID, AssassinGuessRuntime::handleGuess);
    }

    private static void handleGuess(AssassinGuessPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity assassin = context.player();
        ServerWorld world = assassin.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(assassin);
        ServerPlayerEntity target = assassin.getServer().getPlayerManager().getPlayer(payload.targetUuid());
        boolean sameWorld = target != null && target.getServerWorld() == world;
        Role targetRole = sameWorld ? game.getRole(target) : null;
        Optional<Role> guessedRole = StrawRoleMeaning.registeredRole(payload.guessedRoleId());

        AssassinGuessPolicy.GuessAttempt attempt = AssassinGuessPolicy.evaluate(new AssassinGuessPolicy.GuessInput(
                game.isRunning(),
                StrawRoleMeaning.receivesAssassinGuess(game.getRole(assassin)),
                GameFunctions.isPlayerAliveAndSurvival(assassin),
                assassin.getUuid(),
                payload.targetUuid(),
                sameWorld,
                target != null && sameWorld && GameFunctions.isPlayerAliveAndSurvival(target),
                targetRole != null,
                !roleState.isAbilityOnCooldown(AssassinGuessPolicy.ABILITY_ID, currentGameTime),
                AssassinGuessPolicy.guessesRemaining(roleState),
                guessedRole.map(role -> AssassinGuessPolicy.isGuessableRole(role, true)).orElse(false),
                payload.guessedRoleId(),
                StrawRoleMeaning.roleIdFor(targetRole).orElse(null)
        ));
        if (attempt.result().blocked()) {
            sendBlockedMessage(assassin, roleState, currentGameTime, attempt.result());
            return;
        }

        AssassinGuessPolicy.useGuess(roleState, currentGameTime);
        if (attempt.resolution().orElseThrow() == AssassinGuessPolicy.Resolution.CORRECT) {
            GameFunctions.killPlayer(target, true, assassin, StrawDeathReasons.ASSASSINATED);
            assassin.sendMessage(Text.translatable(
                    "message.strawcraft.assassin.correct",
                    target.getDisplayName()
            ).formatted(Formatting.GREEN), true);
        } else {
            GameFunctions.killPlayer(assassin, true, null, StrawDeathReasons.ASSASSIN_MISFIRE);
            assassin.sendMessage(Text.translatable(
                    "message.strawcraft.assassin.wrong",
                    target.getDisplayName()
            ).formatted(Formatting.RED), true);
        }
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity assassin,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            AssassinGuessPolicy.ValidationResult result
    ) {
        switch (result) {
            case COOLDOWN -> assassin.sendMessage(Text.translatable(
                    "message.strawcraft.assassin.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case NO_GUESSES -> assassin.sendMessage(Text.translatable("message.strawcraft.assassin.no_guesses")
                    .formatted(Formatting.RED), true);
            case INVALID_TARGET, TARGET_NOT_ACTIVE -> assassin.sendMessage(
                    Text.translatable("message.strawcraft.assassin.invalid_target").formatted(Formatting.RED),
                    true
            );
            case INVALID_GUESSED_ROLE -> assassin.sendMessage(
                    Text.translatable("message.strawcraft.assassin.invalid_role").formatted(Formatting.RED),
                    true
            );
            default -> {
            }
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(AssassinGuessPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }
}
