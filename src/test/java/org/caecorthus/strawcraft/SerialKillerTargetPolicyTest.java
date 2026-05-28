package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialKillerTargetPolicyTest {
    @Test
    void eligibleTargetsMirrorSparkServerValidationExclusions() {
        UUID serialKiller = UUID.randomUUID();
        UUID eligible = UUID.randomUUID();

        List<UUID> targets = SerialKillerTargetPolicy.eligibleTargets(List.of(
                candidate(serialKiller, true, true, true, false, false, false, false, false),
                candidate(UUID.randomUUID(), false, false, true, false, false, false, false, false),
                candidate(UUID.randomUUID(), false, true, false, false, false, false, false, false),
                candidate(UUID.randomUUID(), false, true, true, true, false, false, false, false),
                candidate(UUID.randomUUID(), false, true, true, false, true, false, false, false),
                candidate(UUID.randomUUID(), false, true, true, false, false, true, false, false),
                candidate(UUID.randomUUID(), false, true, true, false, false, false, true, false),
                candidate(UUID.randomUUID(), false, true, true, false, false, false, false, true),
                candidate(eligible, false, true, true, false, false, false, false, false)
        ));

        assertEquals(List.of(eligible), targets);
    }

    @Test
    void currentTargetIsValidOnlyWhileStillEligible() {
        UUID target = UUID.randomUUID();

        assertTrue(SerialKillerTargetPolicy.isTargetValid(target, List.of(
                candidate(target, false, true, true, false, false, false, false, false)
        )));
        assertEquals(false, SerialKillerTargetPolicy.isTargetValid(target, List.of(
                candidate(target, false, true, false, false, false, false, false, false)
        )));
    }

    @Test
    void assignTargetUsesSourceBackedRandomEligibleSelection() {
        UUID target = UUID.randomUUID();

        Optional<UUID> selected = SerialKillerTargetPolicy.assignTarget(
                List.of(candidate(target, false, true, true, false, false, false, false, false)),
                new FixedRandomGenerator()
        );

        assertEquals(Optional.of(target), selected);
    }

    @Test
    void serialKillerReceivesBonusOnlyForKillingCurrentTarget() {
        UUID killer = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        assertEquals(Optional.of(new KillRewardPolicy.Grant(
                killer,
                SerialKillerTargetPolicy.BONUS_MONEY,
                KillRewardPolicy.GrantReason.SERIAL_KILLER_TARGET
        )), SerialKillerTargetPolicy.bonusGrant(killer, target, Optional.of(target), true));
        assertTrue(SerialKillerTargetPolicy.bonusGrant(killer, UUID.randomUUID(), Optional.of(target), true).isEmpty());
        assertTrue(SerialKillerTargetPolicy.bonusGrant(killer, target, Optional.of(target), false).isEmpty());
    }

    private static SerialKillerTargetPolicy.TargetCandidate candidate(
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
        return new SerialKillerTargetPolicy.TargetCandidate(
                uuid,
                self,
                hasRole,
                alive,
                swallowed,
                killerRole,
                undercover,
                bodyguard,
                survivalMaster
        );
    }

    private static final class FixedRandomGenerator implements RandomGenerator {
        @Override
        public long nextLong() {
            return 0L;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
