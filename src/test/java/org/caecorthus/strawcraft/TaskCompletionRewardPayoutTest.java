package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCompletionRewardPayoutTest {
    @Test
    void payoutAccumulatesMultipleTaskGrantsAndSyncsEachTouchedAccountOnce() {
        UUID recipient = UUID.randomUUID();
        UUID otherRecipient = UUID.randomUUID();
        Map<UUID, TestAccount> accounts = new HashMap<>();
        accounts.put(recipient, new TestAccount(10));
        accounts.put(otherRecipient, new TestAccount(0));

        TaskCompletionRewardPayout.apply(List.of(
                new TaskCompletionRewardPolicy.Grant(recipient, 50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION),
                new TaskCompletionRewardPolicy.Grant(recipient, 50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION),
                new TaskCompletionRewardPolicy.Grant(otherRecipient, 50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION)
        ), accounts::get);

        assertEquals(110, accounts.get(recipient).balance());
        assertEquals(1, accounts.get(recipient).syncs());
        assertEquals(50, accounts.get(otherRecipient).balance());
        assertEquals(1, accounts.get(otherRecipient).syncs());
    }

    @Test
    void payoutAggregatesByUuidBeforeResolvingTransientAccountWrappers() {
        UUID recipient = UUID.randomUUID();
        AtomicInteger balance = new AtomicInteger(10);
        AtomicInteger syncs = new AtomicInteger();
        AtomicInteger lookups = new AtomicInteger();

        TaskCompletionRewardPayout.apply(List.of(
                new TaskCompletionRewardPolicy.Grant(recipient, 50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION),
                new TaskCompletionRewardPolicy.Grant(recipient, 50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION)
        ), uuid -> {
            lookups.incrementAndGet();
            return new TaskCompletionRewardPayout.Account() {
                @Override
                public void addToBalance(int amount) {
                    balance.addAndGet(amount);
                }

                @Override
                public void sync() {
                    syncs.incrementAndGet();
                }
            };
        });

        assertEquals(110, balance.get());
        assertEquals(1, syncs.get());
        assertEquals(1, lookups.get());
    }

    @Test
    void payoutSkipsMissingAccountsAndNonPositiveGrants() {
        AtomicInteger lookups = new AtomicInteger();

        TaskCompletionRewardPayout.apply(List.of(
                new TaskCompletionRewardPolicy.Grant(UUID.randomUUID(), 0, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION),
                new TaskCompletionRewardPolicy.Grant(UUID.randomUUID(), -50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION),
                new TaskCompletionRewardPolicy.Grant(UUID.randomUUID(), 50, TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION)
        ), uuid -> {
            lookups.incrementAndGet();
            return null;
        });

        assertEquals(1, lookups.get());
    }

    private static final class TestAccount implements TaskCompletionRewardPayout.Account {
        private int balance;
        private int syncs;

        private TestAccount(int balance) {
            this.balance = balance;
        }

        @Override
        public void addToBalance(int amount) {
            balance += amount;
        }

        @Override
        public void sync() {
            syncs++;
        }

        private int balance() {
            return balance;
        }

        private int syncs() {
            return syncs;
        }
    }
}
