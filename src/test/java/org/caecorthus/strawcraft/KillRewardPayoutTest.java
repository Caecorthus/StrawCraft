package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KillRewardPayoutTest {
    @Test
    void payoutAccumulatesMultipleGrantsAndSyncsEachTouchedAccountOnce() {
        UUID recipient = UUID.randomUUID();
        UUID teammate = UUID.randomUUID();
        Map<UUID, TestAccount> accounts = new HashMap<>();
        accounts.put(recipient, new TestAccount(10));
        accounts.put(teammate, new TestAccount(0));

        KillRewardPayout.apply(List.of(
                new KillRewardPolicy.Grant(recipient, 100, KillRewardPolicy.GrantReason.DIRECT_KILL),
                new KillRewardPolicy.Grant(recipient, 15, KillRewardPolicy.GrantReason.KILLER_TEAMMATE),
                new KillRewardPolicy.Grant(teammate, 15, KillRewardPolicy.GrantReason.KILLER_TEAMMATE)
        ), accounts::get);

        assertEquals(125, accounts.get(recipient).balance());
        assertEquals(1, accounts.get(recipient).syncs());
        assertEquals(15, accounts.get(teammate).balance());
        assertEquals(1, accounts.get(teammate).syncs());
    }

    @Test
    void payoutAggregatesByUuidBeforeResolvingTransientAccountWrappers() {
        UUID recipient = UUID.randomUUID();
        AtomicInteger balance = new AtomicInteger(10);
        AtomicInteger syncs = new AtomicInteger();
        AtomicInteger lookups = new AtomicInteger();

        KillRewardPayout.apply(List.of(
                new KillRewardPolicy.Grant(recipient, 100, KillRewardPolicy.GrantReason.DIRECT_KILL),
                new KillRewardPolicy.Grant(recipient, 15, KillRewardPolicy.GrantReason.KILLER_TEAMMATE)
        ), uuid -> {
            lookups.incrementAndGet();
            return new KillRewardPayout.Account() {
                @Override
                public int balance() {
                    return balance.get();
                }

                @Override
                public void setBalance(int nextBalance) {
                    balance.set(nextBalance);
                }

                @Override
                public void sync() {
                    syncs.incrementAndGet();
                }
            };
        });

        assertEquals(125, balance.get());
        assertEquals(1, syncs.get());
        assertEquals(1, lookups.get());
    }

    @Test
    void payoutSkipsMissingAccounts() {
        KillRewardPayout.apply(List.of(
                new KillRewardPolicy.Grant(UUID.randomUUID(), 100, KillRewardPolicy.GrantReason.DIRECT_KILL)
        ), uuid -> null);
    }

    private static final class TestAccount implements KillRewardPayout.Account {
        private int balance;
        private int syncs;

        private TestAccount(int balance) {
            this.balance = balance;
        }

        @Override
        public int balance() {
            return balance;
        }

        @Override
        public void setBalance(int balance) {
            this.balance = balance;
        }

        @Override
        public void sync() {
            syncs++;
        }

        private int syncs() {
            return syncs;
        }
    }
}
