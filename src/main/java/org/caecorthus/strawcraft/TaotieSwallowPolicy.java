package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TaotieSwallowPolicy {
    public static final Identifier TAOTIE_ROLE = StrawCraft.id("taotie");
    public static final String ABILITY_ID = "taotie_swallow";
    public static final int BASE_SWALLOW_COOLDOWN_TICKS = 45 * 20;
    public static final int SWALLOW_DISTANCE_SQUARED = 9;
    public static final int TAOTIE_MOMENT_DURATION_TICKS = 2 * 60 * 20;

    private TaotieSwallowPolicy() {
    }

    public static RoundState initializeForGame(int totalPlayers) {
        return new RoundState(
                Set.of(),
                0,
                false,
                0,
                triggerThreshold(totalPlayers),
                totalPlayers,
                calculatedSwallowCooldownTicks(totalPlayers)
        );
    }

    public static int triggerThreshold(int totalPlayers) {
        return Math.max(2, totalPlayers / 5);
    }

    public static int calculatedSwallowCooldownTicks(int totalPlayers) {
        int cooldownSeconds = Math.max(20, Math.min(50, 60 - totalPlayers));
        return cooldownSeconds * 20;
    }

    public static ValidationResult validateSwallow(SwallowInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return ValidationResult.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.taotieRole()) {
            return ValidationResult.NOT_TAOTIE;
        }
        if (!input.taotieAlive() || input.taotieSwallowed()) {
            return ValidationResult.TAOTIE_NOT_ACTIVE;
        }
        if (!input.cooldownReady()) {
            return ValidationResult.COOLDOWN;
        }
        if (!input.targetPresent() || input.selfTarget()) {
            return ValidationResult.INVALID_TARGET;
        }
        if (!input.targetAlivePlaying() || !input.targetAliveSurvival() || input.targetSwallowed()) {
            return ValidationResult.TARGET_NOT_ACTIVE;
        }
        if (!input.sameWorld()) {
            return ValidationResult.DIFFERENT_WORLD;
        }
        if (!input.withinDistance()) {
            return ValidationResult.TOO_FAR;
        }
        if (!input.lineOfSight()) {
            return ValidationResult.NO_LINE_OF_SIGHT;
        }
        return ValidationResult.ALLOWED;
    }

    public static RoundState recordSwallow(RoundState state, UUID targetUuid) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(targetUuid, "targetUuid");
        Set<UUID> swallowedPlayers = new LinkedHashSet<>(state.swallowedPlayers());
        swallowedPlayers.add(targetUuid);
        return new RoundState(
                swallowedPlayers,
                state.calculatedSwallowCooldownTicks(),
                state.taotieMomentActive(),
                state.taotieMomentTicks(),
                state.triggerThreshold(),
                state.totalPlayersAtStart(),
                state.calculatedSwallowCooldownTicks()
        );
    }

    public static RoundState releasePlayer(RoundState state, UUID targetUuid) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(targetUuid, "targetUuid");
        Set<UUID> swallowedPlayers = new LinkedHashSet<>(state.swallowedPlayers());
        swallowedPlayers.remove(targetUuid);
        return state.withSwallowedPlayers(swallowedPlayers);
    }

    public static RoundState releaseAllPlayers(RoundState state) {
        Objects.requireNonNull(state, "state");
        return state.withSwallowedPlayers(Set.of());
    }

    public static RoundState checkAndTriggerMoment(RoundState state, int aliveCount) {
        Objects.requireNonNull(state, "state");
        if (state.taotieMomentActive() || state.triggerThreshold() < 2 || aliveCount > state.triggerThreshold()) {
            return state;
        }
        return new RoundState(
                state.swallowedPlayers(),
                state.swallowCooldownTicks(),
                true,
                TAOTIE_MOMENT_DURATION_TICKS,
                state.triggerThreshold(),
                state.totalPlayersAtStart(),
                state.calculatedSwallowCooldownTicks()
        );
    }

    public static RoundState tick(RoundState state) {
        Objects.requireNonNull(state, "state");
        int cooldownTicks = Math.max(0, state.swallowCooldownTicks() - 1);
        int momentTicks = state.taotieMomentActive() ? Math.max(0, state.taotieMomentTicks() - 1) : 0;
        return new RoundState(
                state.swallowedPlayers(),
                cooldownTicks,
                state.taotieMomentActive(),
                momentTicks,
                state.triggerThreshold(),
                state.totalPlayersAtStart(),
                state.calculatedSwallowCooldownTicks()
        );
    }

    public static boolean hasTaotieMomentCompleted(RoundState state) {
        Objects.requireNonNull(state, "state");
        return state.taotieMomentActive() && state.taotieMomentTicks() <= 0;
    }

    public static boolean hasSwallowedEveryone(UUID taotieUuid, RoundState state, List<ParticipantStatus> participants) {
        Objects.requireNonNull(taotieUuid, "taotieUuid");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(participants, "participants");
        if (state.swallowedPlayers().isEmpty()) {
            return false;
        }
        return participants.stream()
                .filter(ParticipantStatus::alive)
                .filter(participant -> !participant.uuid().equals(taotieUuid))
                .allMatch(participant -> state.swallowedPlayers().contains(participant.uuid()));
    }

    public enum ValidationResult {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_TAOTIE,
        TAOTIE_NOT_ACTIVE,
        COOLDOWN,
        INVALID_TARGET,
        TARGET_NOT_ACTIVE,
        DIFFERENT_WORLD,
        TOO_FAR,
        NO_LINE_OF_SIGHT;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record SwallowInput(
            boolean roundRunning,
            boolean taotieRole,
            boolean taotieAlive,
            boolean taotieSwallowed,
            boolean cooldownReady,
            boolean targetPresent,
            boolean selfTarget,
            boolean targetAlivePlaying,
            boolean targetAliveSurvival,
            boolean targetSwallowed,
            boolean sameWorld,
            boolean withinDistance,
            boolean lineOfSight
    ) {
        public SwallowInput withRoundRunning(boolean value) {
            return new SwallowInput(value, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTaotieRole(boolean value) {
            return new SwallowInput(roundRunning, value, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTaotieAlive(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, value, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTaotieSwallowed(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, value, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withCooldownReady(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, value,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTargetPresent(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    value, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withSelfTarget(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, value, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTargetAlivePlaying(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, value, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTargetAliveSurvival(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, value, targetSwallowed,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withTargetSwallowed(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, value,
                    sameWorld, withinDistance, lineOfSight);
        }

        public SwallowInput withSameWorld(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    value, withinDistance, lineOfSight);
        }

        public SwallowInput withWithinDistance(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, value, lineOfSight);
        }

        public SwallowInput withLineOfSight(boolean value) {
            return new SwallowInput(roundRunning, taotieRole, taotieAlive, taotieSwallowed, cooldownReady,
                    targetPresent, selfTarget, targetAlivePlaying, targetAliveSurvival, targetSwallowed,
                    sameWorld, withinDistance, value);
        }
    }

    public record RoundState(
            Set<UUID> swallowedPlayers,
            int swallowCooldownTicks,
            boolean taotieMomentActive,
            int taotieMomentTicks,
            int triggerThreshold,
            int totalPlayersAtStart,
            int calculatedSwallowCooldownTicks
    ) {
        public RoundState {
            // Taotie foundation stores only server-owned UUID state; spectator/camera behavior is intentionally absent.
            // 饕餮基础层只保存服务端 UUID 状态；旁观者/镜头行为暂不实现。
            Objects.requireNonNull(swallowedPlayers, "swallowedPlayers");
            swallowedPlayers = Set.copyOf(swallowedPlayers);
        }

        private RoundState withSwallowedPlayers(Set<UUID> nextSwallowedPlayers) {
            return new RoundState(
                    nextSwallowedPlayers,
                    swallowCooldownTicks,
                    taotieMomentActive,
                    taotieMomentTicks,
                    triggerThreshold,
                    totalPlayersAtStart,
                    calculatedSwallowCooldownTicks
            );
        }
    }

    public record ParticipantStatus(UUID uuid, boolean alive) {
        public ParticipantStatus {
            Objects.requireNonNull(uuid, "uuid");
        }
    }
}
