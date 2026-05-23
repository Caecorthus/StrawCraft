package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KillRewardPolicyTest {
    @Test
    void directKillerReceivesMainRewardAndLivingKillerTeammatesReceiveAddonReward() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();
        UUID deadTeammate = UUID.randomUUID();

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, false, killer, false),
                List.of(
                        new KillRewardPolicy.KillerParticipant(killer, true),
                        new KillRewardPolicy.KillerParticipant(teammate, true),
                        new KillRewardPolicy.KillerParticipant(deadTeammate, false)
                )
        );

        assertEquals(List.of(
                new KillRewardPolicy.Grant(killer, GameConstants.MONEY_PER_KILL, KillRewardPolicy.GrantReason.DIRECT_KILL),
                new KillRewardPolicy.Grant(teammate, KillRewardPolicy.TEAMMATE_KILL_REWARD, KillRewardPolicy.GrantReason.KILLER_TEAMMATE)
        ), grants);
    }

    @Test
    void indirectAttributionMarksRecipientAndStillPaysLivingKillerTeammates() {
        UUID victim = UUID.randomUUID();
        UUID poisoner = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, poisoner, true),
                List.of(
                        new KillRewardPolicy.KillerParticipant(poisoner, true),
                        new KillRewardPolicy.KillerParticipant(teammate, true)
                )
        );

        assertEquals(List.of(
                new KillRewardPolicy.Grant(poisoner, GameConstants.MONEY_PER_KILL, KillRewardPolicy.GrantReason.INDIRECT_KILL),
                new KillRewardPolicy.Grant(teammate, KillRewardPolicy.TEAMMATE_KILL_REWARD, KillRewardPolicy.GrantReason.KILLER_TEAMMATE)
        ), grants);
    }

    @Test
    void unattributedGoodVictimDeathSplitsPoolAcrossLivingKillersWithStableRemainder() {
        UUID victim = UUID.fromString("00000000-0000-0000-0000-000000000099");
        UUID firstKiller = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID secondKiller = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID thirdKiller = UUID.fromString("00000000-0000-0000-0000-000000000002");

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, null, false),
                List.of(
                        new KillRewardPolicy.KillerParticipant(firstKiller, true),
                        new KillRewardPolicy.KillerParticipant(secondKiller, true),
                        new KillRewardPolicy.KillerParticipant(thirdKiller, true)
                )
        );

        assertEquals(List.of(
                new KillRewardPolicy.Grant(secondKiller, 34, KillRewardPolicy.GrantReason.GOOD_VICTIM_POOL),
                new KillRewardPolicy.Grant(thirdKiller, 33, KillRewardPolicy.GrantReason.GOOD_VICTIM_POOL),
                new KillRewardPolicy.Grant(firstKiller, 33, KillRewardPolicy.GrantReason.GOOD_VICTIM_POOL)
        ), grants);
    }

    @Test
    void unattributedNonGoodVictimDeathDoesNotPayTheKillerTeamPool() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        assertEquals(List.of(), KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, false, null, false),
                List.of(new KillRewardPolicy.KillerParticipant(killer, true))
        ));
    }

    @Test
    void unattributedGoodVictimDeathDoesNotPayWhenNoLivingKillersRemain() {
        UUID victim = UUID.randomUUID();
        UUID deadKiller = UUID.randomUUID();

        assertEquals(List.of(), KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, null, false),
                List.of(new KillRewardPolicy.KillerParticipant(deadKiller, false))
        ));
    }

    @Test
    void attributedKillerWhoIsTheVictimReceivesNoReward() {
        UUID victim = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();

        assertEquals(List.of(), KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, victim, false),
                List.of(
                        new KillRewardPolicy.KillerParticipant(victim, true),
                        new KillRewardPolicy.KillerParticipant(teammate, true)
                )
        ));
    }

    @Test
    void attributedKillerWhoIsNotLivingStillReceivesMainRewardAndTriggersLivingTeammateRewards() {
        UUID victim = UUID.randomUUID();
        UUID notLivingKiller = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, notLivingKiller, false),
                List.of(
                        new KillRewardPolicy.KillerParticipant(notLivingKiller, false),
                        new KillRewardPolicy.KillerParticipant(teammate, true)
                )
        );

        assertEquals(List.of(
                new KillRewardPolicy.Grant(notLivingKiller, GameConstants.MONEY_PER_KILL, KillRewardPolicy.GrantReason.DIRECT_KILL),
                new KillRewardPolicy.Grant(teammate, KillRewardPolicy.TEAMMATE_KILL_REWARD, KillRewardPolicy.GrantReason.KILLER_TEAMMATE)
        ), grants);
    }

    @Test
    void attributedKillerOutsideKillerTeamReceivesNoRewardAndDoesNotTriggerTeammateRewards() {
        UUID victim = UUID.randomUUID();
        UUID nonKiller = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();

        assertEquals(List.of(), KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, nonKiller, false),
                List.of(new KillRewardPolicy.KillerParticipant(teammate, true))
        ));
    }

    @Test
    void teammateRewardsExcludeVictimDeadPlayersAndPlayersOutsideKillerTeam() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        UUID livingTeammate = UUID.randomUUID();
        UUID deadTeammate = UUID.randomUUID();
        UUID nonKiller = UUID.randomUUID();

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, false, killer, false),
                List.of(
                        new KillRewardPolicy.KillerParticipant(killer, true),
                        new KillRewardPolicy.KillerParticipant(victim, true),
                        new KillRewardPolicy.KillerParticipant(livingTeammate, true),
                        new KillRewardPolicy.KillerParticipant(deadTeammate, false)
                )
        );

        assertEquals(List.of(
                new KillRewardPolicy.Grant(killer, GameConstants.MONEY_PER_KILL, KillRewardPolicy.GrantReason.DIRECT_KILL),
                new KillRewardPolicy.Grant(livingTeammate, KillRewardPolicy.TEAMMATE_KILL_REWARD, KillRewardPolicy.GrantReason.KILLER_TEAMMATE)
        ), grants);
        assertEquals(false, grants.stream().anyMatch(grant -> grant.recipientUuid().equals(nonKiller)));
    }

    @Test
    void splitRewardsExcludeVictimDeadPlayersAndPlayersOutsideKillerTeam() {
        UUID victim = UUID.randomUUID();
        UUID livingKiller = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID deadKiller = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID nonKiller = UUID.fromString("00000000-0000-0000-0000-000000000003");

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(victim, true, null, false),
                List.of(
                        new KillRewardPolicy.KillerParticipant(victim, true),
                        new KillRewardPolicy.KillerParticipant(livingKiller, true),
                        new KillRewardPolicy.KillerParticipant(deadKiller, false)
                )
        );

        assertEquals(List.of(
                new KillRewardPolicy.Grant(livingKiller, GameConstants.MONEY_PER_KILL, KillRewardPolicy.GrantReason.GOOD_VICTIM_POOL)
        ), grants);
        assertEquals(false, grants.stream().anyMatch(grant -> grant.recipientUuid().equals(nonKiller)));
    }
}
