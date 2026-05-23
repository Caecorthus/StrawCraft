package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class KillRewardPolicy {
    public static final int TEAMMATE_KILL_REWARD = 15;

    private KillRewardPolicy() {
    }

    public static List<Grant> compute(Context context, List<KillerParticipant> killerTeam) {
        List<KillerParticipant> livingKillers = killerTeam.stream()
                .filter(KillerParticipant::alive)
                .filter(participant -> !participant.uuid().equals(context.victimUuid()))
                .sorted(Comparator.comparing(KillerParticipant::uuid))
                .toList();

        if (context.attributedKillerUuid() != null) {
            return attributedKillGrants(context, killerTeam, livingKillers);
        }
        if (context.victimGood()) {
            return goodVictimPoolGrants(livingKillers);
        }
        return List.of();
    }

    private static List<Grant> attributedKillGrants(
            Context context,
            List<KillerParticipant> killerTeam,
            List<KillerParticipant> livingKillers
    ) {
        // Direct Wathe-like kill rewards require killer feature eligibility, not current survival state.
        // 直接击杀奖励沿用 Wathe 风格：需要杀手功能资格，但不要求此刻仍然存活。
        boolean attributedKillerCanUseKillerFeatures = killerTeam.stream()
                .filter(participant -> !participant.uuid().equals(context.victimUuid()))
                .anyMatch(participant -> participant.uuid().equals(context.attributedKillerUuid()));
        if (!attributedKillerCanUseKillerFeatures) {
            return List.of();
        }

        List<Grant> grants = new ArrayList<>();
        grants.add(new Grant(
                context.attributedKillerUuid(),
                GameConstants.MONEY_PER_KILL,
                context.indirectAttribution() ? GrantReason.INDIRECT_KILL : GrantReason.DIRECT_KILL
        ));
        for (KillerParticipant participant : livingKillers) {
            if (!participant.uuid().equals(context.attributedKillerUuid())) {
                // Teammate rewards are StrawCraft-owned and only go to online, living killer teammates.
                // 队友奖励归 StrawCraft 管，只发给在线且存活的杀手队友。
                grants.add(new Grant(participant.uuid(), TEAMMATE_KILL_REWARD, GrantReason.KILLER_TEAMMATE));
            }
        }
        return List.copyOf(grants);
    }

    private static List<Grant> goodVictimPoolGrants(List<KillerParticipant> livingKillers) {
        if (livingKillers.isEmpty()) {
            return List.of();
        }

        int baseShare = GameConstants.MONEY_PER_KILL / livingKillers.size();
        int remainder = GameConstants.MONEY_PER_KILL % livingKillers.size();
        List<Grant> grants = new ArrayList<>();
        for (int index = 0; index < livingKillers.size(); index++) {
            int amount = baseShare + (index < remainder ? 1 : 0);
            if (amount > 0) {
                grants.add(new Grant(livingKillers.get(index).uuid(), amount, GrantReason.GOOD_VICTIM_POOL));
            }
        }
        return List.copyOf(grants);
    }

    public record Context(
            UUID victimUuid,
            boolean victimGood,
            @Nullable UUID attributedKillerUuid,
            boolean indirectAttribution
    ) {
    }

    public record KillerParticipant(UUID uuid, boolean alive) {
    }

    public record Grant(UUID recipientUuid, int amount, GrantReason reason) {
    }

    public enum GrantReason {
        DIRECT_KILL,
        INDIRECT_KILL,
        KILLER_TEAMMATE,
        GOOD_VICTIM_POOL
    }
}
