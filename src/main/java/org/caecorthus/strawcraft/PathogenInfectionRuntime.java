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
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PathogenInfectionRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private PathogenInfectionRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(PathogenInfectionPayload.ID, PathogenInfectionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PathogenInfectionPayload.ID, PathogenInfectionRuntime::handleInfection);
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(PathogenInfectionRuntime::checkPathogenClaimsAfterDeath);
    }

    private static void handleInfection(PathogenInfectionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity pathogen = context.player();
        ServerWorld world = pathogen.getServerWorld();
        long currentGameTime = world.getTime();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        NoellesRoleStateComponent roleState = NoellesRoleStateComponent.KEY.get(pathogen);

        PathogenInfectionPolicy.InfectionAttempt attempt = PathogenInfectionPolicy.evaluate(
                new PathogenInfectionPolicy.ActivationInput(
                        game.isRunning(),
                        isPathogenRole(game.getRole(pathogen)),
                        GameFunctions.isPlayerAliveAndSurvival(pathogen),
                        !roleState.isAbilityOnCooldown(PathogenInfectionPolicy.ABILITY_ID, currentGameTime),
                        pathogen.getUuid(),
                        targetCandidates(world, game, pathogen)
                )
        );
        if (attempt.result().blocked()) {
            sendBlockedMessage(pathogen, roleState, currentGameTime, attempt.result());
            return;
        }

        if (!(world.getPlayerByUuid(attempt.targetUuid().orElseThrow()) instanceof ServerPlayerEntity target)) {
            sendBlockedMessage(pathogen, roleState, currentGameTime, PathogenInfectionPolicy.ValidationResult.NO_TARGET);
            return;
        }

        NoellesRoleStateComponent targetState = NoellesRoleStateComponent.KEY.get(target);
        targetState.setPathogenInfectedBy(pathogen.getUuid());
        roleState.tryBeginAbilityCooldown(
                PathogenInfectionPolicy.ABILITY_ID,
                currentGameTime,
                PathogenInfectionPolicy.baseCooldownTicks(roleState, game.getRoles().size())
        );

        boolean claimed = recordPathogenWinIfComplete(world, game, pathogen, currentGameTime);
        pathogen.sendMessage(Text.translatable(
                claimed ? "message.strawcraft.pathogen.infected_claimed" : "message.strawcraft.pathogen.infected",
                target.getDisplayName()
        ).formatted(claimed ? Formatting.GREEN : Formatting.AQUA), true);
    }

    private static List<PathogenInfectionPolicy.TargetCandidate> targetCandidates(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity pathogen
    ) {
        return world.getPlayers().stream()
                .map(target -> targetCandidate(game, pathogen, target))
                .toList();
    }

    private static PathogenInfectionPolicy.TargetCandidate targetCandidate(
            GameWorldComponent game,
            ServerPlayerEntity pathogen,
            ServerPlayerEntity target
    ) {
        NoellesRoleStateComponent targetState = NoellesRoleStateComponent.KEY.get(target);
        return new PathogenInfectionPolicy.TargetCandidate(
                target.getUuid(),
                game.getRole(target) != null,
                GameFunctions.isPlayerAliveAndSurvival(target),
                targetState.pathogenInfectedBy().isPresent(),
                pathogen.canSee(target),
                pathogen.squaredDistanceTo(target)
        );
    }

    private static void checkPathogenClaimsAfterDeath(StrawDeathEvents.RoleDeathContext context) {
        ServerWorld world = context.world();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!game.isRunning()) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (isPathogenRole(game.getRole(player)) && GameFunctions.isPlayerAliveAndSurvival(player)) {
                if (recordPathogenWinIfComplete(world, game, player, context.official().gameTime())) {
                    player.sendMessage(Text.translatable("message.strawcraft.pathogen.claimed_win")
                            .formatted(Formatting.GREEN), true);
                }
            }
        }
    }

    private static boolean recordPathogenWinIfComplete(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity pathogen,
            long currentGameTime
    ) {
        return PathogenWinPolicy.recordNeutralWinIfComplete(
                NoellesRoleStateComponent.KEY.get(pathogen),
                pathogen.getUuid(),
                winParticipants(world, game),
                currentGameTime
        );
    }

    private static List<PathogenWinPolicy.Participant> winParticipants(ServerWorld world, GameWorldComponent game) {
        return world.getPlayers().stream()
                .map(player -> winParticipant(game, player))
                .toList();
    }

    private static PathogenWinPolicy.Participant winParticipant(GameWorldComponent game, ServerPlayerEntity player) {
        Role role = game.getRole(player);
        return new PathogenWinPolicy.Participant(
                player.getUuid(),
                role != null,
                GameFunctions.isPlayerAliveAndSurvival(player),
                isPathogenRole(role),
                NoellesRoleStateComponent.KEY.get(player).pathogenInfectedBy()
        );
    }

    private static void sendBlockedMessage(
            ServerPlayerEntity pathogen,
            NoellesRoleStateComponent roleState,
            long currentGameTime,
            PathogenInfectionPolicy.ValidationResult result
    ) {
        switch (result) {
            case COOLDOWN -> pathogen.sendMessage(Text.translatable(
                    "message.strawcraft.pathogen.cooldown",
                    secondsRemaining(roleState, currentGameTime)
            ).formatted(Formatting.YELLOW), true);
            case NO_TARGET -> pathogen.sendMessage(Text.translatable("message.strawcraft.pathogen.no_target")
                    .formatted(Formatting.RED), true);
            default -> {
            }
        }
    }

    private static int secondsRemaining(NoellesRoleStateComponent roleState, long currentGameTime) {
        int ticks = roleState.getRemainingAbilityCooldown(PathogenInfectionPolicy.ABILITY_ID, currentGameTime);
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    private static boolean isPathogenRole(@Nullable Role role) {
        return StrawRoleMeaning.receivesPathogenInfection(role);
    }
}
