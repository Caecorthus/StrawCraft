package org.caecorthus.strawcraft;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class SerialKillerTargetPolicy {
    public static final int BONUS_MONEY = 50;

    private SerialKillerTargetPolicy() {
    }

    public static Optional<UUID> assignTarget(List<TargetCandidate> candidates, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<UUID> eligibleTargets = eligibleTargets(candidates);
        if (eligibleTargets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(eligibleTargets.get(random.nextInt(eligibleTargets.size())));
    }

    public static boolean isTargetValid(UUID targetUuid, List<TargetCandidate> candidates) {
        Objects.requireNonNull(targetUuid, "targetUuid");
        return candidates.stream()
                .filter(candidate -> targetUuid.equals(candidate.uuid()))
                .anyMatch(SerialKillerTargetPolicy::isEligible);
    }

    public static List<UUID> eligibleTargets(List<TargetCandidate> candidates) {
        // Spark parity: targets must be living non-killer roles, excluding protected Noelles roles.
        // 对齐 Spark：目标必须是存活的非杀手职业，并排除受保护的 Noelles 职业。
        return candidates.stream()
                .filter(SerialKillerTargetPolicy::isEligible)
                .map(TargetCandidate::uuid)
                .toList();
    }

    public static Optional<KillRewardPolicy.Grant> bonusGrant(
            UUID serialKillerUuid,
            UUID victimUuid,
            Optional<UUID> currentTarget,
            boolean serialKillerRole
    ) {
        // The bonus is additive to Wathe's normal kill payout and only fires for the current target.
        // 该奖励叠加在 Wathe 常规击杀奖励之外，且只在击杀当前目标时触发。
        Objects.requireNonNull(serialKillerUuid, "serialKillerUuid");
        Objects.requireNonNull(victimUuid, "victimUuid");
        Objects.requireNonNull(currentTarget, "currentTarget");
        if (!serialKillerRole || currentTarget.filter(victimUuid::equals).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new KillRewardPolicy.Grant(
                serialKillerUuid,
                BONUS_MONEY,
                KillRewardPolicy.GrantReason.SERIAL_KILLER_TARGET
        ));
    }

    private static boolean isEligible(TargetCandidate candidate) {
        return !candidate.self()
                && candidate.hasRole()
                && candidate.alive()
                && !candidate.swallowed()
                && !candidate.killerRole()
                && !candidate.undercover()
                && !candidate.bodyguard()
                && !candidate.survivalMaster();
    }

    public record TargetCandidate(
            UUID uuid,
            boolean self,
            boolean hasRole,
            boolean alive,
            boolean swallowed,
            boolean killerRole,
            boolean undercover,
            boolean bodyguard,
            boolean survivalMaster
    ) {
        public TargetCandidate {
            Objects.requireNonNull(uuid, "uuid");
        }
    }
}
