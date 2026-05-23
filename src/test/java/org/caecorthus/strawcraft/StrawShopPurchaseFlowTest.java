package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.util.ShopEntry;
import org.caecorthus.strawcraft.api.StrawShopEvents;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawShopPurchaseFlowTest {
    @Test
    void buildEntriesRunsBeforeLegacyModifyEntriesInIndexOrder() {
        StrawShopEntry base = entry("build-order-base", 100, 0, -1);
        StrawShopEntry built = entry("build-order-built", 200, 0, -1);
        StrawShopEntry modified = entry("build-order-modified", 300, 0, -1);
        StrawShopEvents.BUILD_ENTRIES.register(context -> {
            if (contains(context, "build-order-base")) {
                context.addEntry(built);
            }
        });
        StrawShopEvents.MODIFY_ENTRIES.register((player, context) -> {
            if (contains(context, "build-order-built")) {
                context.setEntry(1, modified);
            }
        });

        List<ShopEntry> entries = StrawShopEvents.buildEntries(null, List.of(base));

        assertEquals(List.of("build-order-base", "build-order-modified"), entries.stream()
                .map(StrawShopEntry::idFor)
                .toList());
    }

    @Test
    void buildEntriesApiIsGlobalSoListenersCannotBranchOnPlayerSpecificState() throws NoSuchMethodException {
        Method buildEntries = StrawShopEvents.BuildEntries.class.getMethod(
                "buildEntries",
                StrawShopEvents.ShopContext.class
        );

        assertEquals(1, buildEntries.getParameterCount());
    }

    @Test
    void purchaseUsesAlreadyMaterializedEntriesWithoutRunningBuilderAgain() {
        StrawShopEntry base = entry("materialized-base", 100, 0, -1);
        AtomicInteger builds = new AtomicInteger();
        StrawShopEvents.BUILD_ENTRIES.register(context -> {
            if (contains(context, "materialized-base")) {
                builds.incrementAndGet();
                context.addEntry(0, entry("materialized-inserted-" + builds.get(), 100, 0, -1));
            }
        });

        List<ShopEntry> materializedEntries = StrawShopEvents.buildEntries(List.of(base));
        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                1,
                materializedEntries,
                new TestAccount(100),
                (shopEntry, player) -> "materialized-base".equals(StrawShopEntry.idFor(shopEntry)),
                TestState.NOOP,
                TestShopAccess.ALLOW,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, status);
        assertEquals(1, builds.get());
    }

    @Test
    void deniedPurchaseDoesNotChargeDeliverOrRecordState() {
        StrawShopEntry entry = entry("deny-flow", 300, 40, 2);
        TestAccount account = new TestAccount(300);
        TestState state = new TestState(true);
        AtomicInteger deliveries = new AtomicInteger();
        StrawShopEvents.BEFORE_PURCHASE.register((player, context) -> {
            if ("deny-flow".equals(StrawShopEntry.idFor(context.entry()))) {
                context.deny();
            }
        });

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(entry),
                account,
                (shopEntry, player) -> {
                    deliveries.incrementAndGet();
                    return true;
                },
                state,
                TestShopAccess.ALLOW,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.DENIED, status);
        assertEquals(300, account.balance());
        assertEquals(0, deliveries.get());
        assertEquals(0, state.records());
    }

    @Test
    void nonKillerCannotBuyCustomStrawEntryEvenWithBalance() {
        StrawShopEntry entry = entry("non-killer-custom", 100, 0, -1);
        TestAccount account = new TestAccount(100);
        AtomicInteger deliveries = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        StrawShopEvents.AFTER_PURCHASE.register((player, receipt) -> {
            if ("non-killer-custom".equals(StrawShopEntry.idFor(receipt.entry()))) {
                afterCalls.incrementAndGet();
            }
        });

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(entry),
                account,
                (shopEntry, player) -> {
                    deliveries.incrementAndGet();
                    return true;
                },
                TestState.NOOP,
                TestShopAccess.DENY,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SHOP_ACCESS_DENIED, status);
        assertEquals(100, account.balance());
        assertEquals(0, deliveries.get());
        assertEquals(0, afterCalls.get());
    }

    @Test
    void killerCanBuyCustomStrawEntry() {
        StrawShopEntry entry = entry("killer-custom", 100, 0, -1);
        TestAccount account = new TestAccount(100);
        AtomicInteger deliveries = new AtomicInteger();

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(entry),
                account,
                (shopEntry, player) -> {
                    deliveries.incrementAndGet();
                    return true;
                },
                TestState.NOOP,
                TestShopAccess.ALLOW,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, status);
        assertEquals(0, account.balance());
        assertEquals(1, deliveries.get());
    }

    @Test
    void cooledDownDecoratedOfficialEntryIsRejectedBeforeDeliveryChargeAfterOrStateRecord() {
        ShopEntry official = new ShopEntry(null, 100, ShopEntry.Type.TOOL);
        ShopEntry decorated = StrawShopEntry.decorate(official);
        TestAccount account = new TestAccount(100);
        TestState state = new TestState(true);
        AtomicInteger deliveries = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        StrawShopEvents.AFTER_PURCHASE.register((player, receipt) -> {
            if (receipt.entry() == decorated) {
                afterCalls.incrementAndGet();
            }
        });

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(decorated),
                account,
                (shopEntry, player) -> {
                    deliveries.incrementAndGet();
                    return true;
                },
                state,
                TestShopAccess.ALLOW,
                TestItemCooldowns.COOLING_DOWN,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.ITEM_COOLDOWN, status);
        assertEquals(100, account.balance());
        assertEquals(0, deliveries.get());
        assertEquals(0, afterCalls.get());
        assertEquals(0, state.records());
    }

    @Test
    void uncooledDecoratedOfficialEntryCanStillPurchase() {
        ShopEntry decorated = StrawShopEntry.decorate(new ShopEntry(null, 100, ShopEntry.Type.TOOL));
        TestAccount account = new TestAccount(100);

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(decorated),
                account,
                (shopEntry, player) -> true,
                TestState.NOOP,
                TestShopAccess.ALLOW,
                TestItemCooldowns.READY,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, status);
        assertEquals(0, account.balance());
    }

    @Test
    void cooledDownCustomStrawEntryIsRejectedBeforeDeliveryChargeAfterOrStateRecord() {
        StrawShopEntry entry = entry("custom-cooldown", 100, 40, 2);
        TestAccount account = new TestAccount(100);
        TestState state = new TestState(true);
        AtomicInteger deliveries = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        StrawShopEvents.AFTER_PURCHASE.register((player, receipt) -> {
            if ("custom-cooldown".equals(StrawShopEntry.idFor(receipt.entry()))) {
                afterCalls.incrementAndGet();
            }
        });

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(entry),
                account,
                (shopEntry, player) -> {
                    deliveries.incrementAndGet();
                    return true;
                },
                state,
                TestShopAccess.ALLOW,
                TestItemCooldowns.COOLING_DOWN,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.ITEM_COOLDOWN, status);
        assertEquals(100, account.balance());
        assertEquals(0, deliveries.get());
        assertEquals(0, afterCalls.get());
        assertEquals(0, state.records());
    }

    @Test
    void discountCanSucceedWhenBalanceIsBelowOriginalPriceButCoversAdjustedPrice() {
        StrawShopEntry discountEntry = entry("discount-below-original", 300, 0, -1);
        TestAccount account = new TestAccount(100);
        StrawShopEvents.BEFORE_PURCHASE.register((player, context) -> {
            if ("discount-below-original".equals(StrawShopEntry.idFor(context.entry()))) {
                context.allow(100);
            }
        });

        StrawShopPurchaseFlow.PurchaseStatus status = buy(discountEntry, account);

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, status);
        assertEquals(0, account.balance());
    }

    @Test
    void allowZeroAndModifiedPriceChargeAdjustedPriceOnce() {
        StrawShopEntry freeEntry = entry("allow-free", 300, 0, -1);
        StrawShopEntry discountEntry = entry("allow-discount", 300, 0, -1);
        TestAccount account = new TestAccount(600);
        StrawShopEvents.BEFORE_PURCHASE.register((player, context) -> {
            if ("allow-free".equals(StrawShopEntry.idFor(context.entry()))) {
                context.allow(0);
            }
            if ("allow-discount".equals(StrawShopEntry.idFor(context.entry()))) {
                context.allow(100);
            }
        });

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, buy(freeEntry, account));
        assertEquals(600, account.balance());
        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, buy(discountEntry, account));
        assertEquals(500, account.balance());
    }

    @Test
    void afterPurchaseOnlyFiresAfterSuccess() {
        StrawShopEntry failed = entry("after-delivery-failed", 100, 0, -1);
        StrawShopEntry successful = entry("after-delivery-success", 100, 0, -1);
        AtomicInteger afterCalls = new AtomicInteger();
        StrawShopEvents.AFTER_PURCHASE.register((player, receipt) -> {
            if (StrawShopEntry.idFor(receipt.entry()).startsWith("after-delivery-")) {
                afterCalls.incrementAndGet();
            }
        });

        StrawShopPurchaseFlow.tryBuy(null, 0, List.of(failed), new TestAccount(100), (entry, player) -> false, TestState.NOOP, TestShopAccess.ALLOW, player -> {
        });
        StrawShopPurchaseFlow.tryBuy(null, 0, List.of(successful), new TestAccount(100), (entry, player) -> true, TestState.NOOP, TestShopAccess.ALLOW, player -> {
        });

        assertEquals(1, afterCalls.get());
    }

    @Test
    void deliveryFailureDoesNotChargeOrRunAfterPurchase() {
        StrawShopEntry entry = entry("delivery-failure", 100, 0, -1);
        TestAccount account = new TestAccount(100);
        AtomicInteger afterCalls = new AtomicInteger();
        StrawShopEvents.AFTER_PURCHASE.register((player, receipt) -> {
            if ("delivery-failure".equals(StrawShopEntry.idFor(receipt.entry()))) {
                afterCalls.incrementAndGet();
            }
        });

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null, 0, List.of(entry), account, (shopEntry, player) -> false, TestState.NOOP, TestShopAccess.ALLOW, player -> {
                });

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.DELIVERY_FAILED, status);
        assertEquals(100, account.balance());
        assertEquals(0, afterCalls.get());
    }

    @Test
    void stockAndCooldownRecordOnlyOnceAfterSuccessfulPurchase() {
        StrawShopEntry entry = entry("state-once", 100, 40, 2);
        TestState state = new TestState(true);

        StrawShopPurchaseFlow.PurchaseStatus status = StrawShopPurchaseFlow.tryBuy(
                null,
                0,
                List.of(entry),
                new TestAccount(100),
                (shopEntry, player) -> true,
                state,
                TestShopAccess.ALLOW,
                player -> {
                }
        );

        assertEquals(StrawShopPurchaseFlow.PurchaseStatus.SUCCESS, status);
        assertEquals(1, state.records());
        assertEquals(1, state.ensures());
        assertEquals(1, state.canPurchases());
    }

    @Test
    void p320ShopEntryKeepsPriceCooldownStockAndIdCompatibility() {
        StrawShopEntry p320 = entry("p320", 300, 40, 2);

        StrawShopEvents.PurchaseContext context = StrawShopEvents.beforePurchase(null, p320, 1);

        assertTrue(context.allowed());
        assertEquals(300, context.price());
        assertEquals(40, p320.cooldownTicks());
        assertEquals(2, p320.maxStock());
        assertEquals("p320", StrawShopEntry.idFor(p320));
    }

    private static StrawShopPurchaseFlow.PurchaseStatus buy(StrawShopEntry entry, TestAccount account) {
        return StrawShopPurchaseFlow.tryBuy(null, 0, List.of(entry), account, (shopEntry, player) -> true, TestState.NOOP, TestShopAccess.ALLOW, player -> {
        });
    }

    private static boolean contains(StrawShopEvents.ShopContext context, String id) {
        return context.getEntries().stream().map(StrawShopEntry::idFor).anyMatch(id::equals);
    }

    private static StrawShopEntry entry(String id, int price, int cooldownTicks, int maxStock) {
        return new StrawShopEntry(id, null, null, price, ShopEntry.Type.WEAPON, cooldownTicks, 0, maxStock);
    }

    private static final class TestAccount implements StrawShopPurchaseFlow.Account {
        private int balance;

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
        }
    }

    private static final class TestState implements StrawShopPurchaseFlow.ShopStateAccess {
        private static final TestState NOOP = new TestState(true);
        private final boolean canPurchase;
        private int ensures;
        private int canPurchases;
        private int records;

        private TestState(boolean canPurchase) {
            this.canPurchase = canPurchase;
        }

        @Override
        public void ensureEntry(StrawShopEntry entry, long now) {
            ensures++;
        }

        @Override
        public boolean canPurchase(StrawShopEntry entry, long now) {
            canPurchases++;
            return canPurchase;
        }

        @Override
        public void recordPurchase(StrawShopEntry entry, long now) {
            records++;
        }

        private int ensures() {
            return ensures;
        }

        private int canPurchases() {
            return canPurchases;
        }

        private int records() {
            return records;
        }
    }

    private enum TestShopAccess implements StrawShopPurchaseFlow.ShopAccess {
        ALLOW(true),
        DENY(false);

        private final boolean allowed;

        TestShopAccess(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean canUseShop(@org.jetbrains.annotations.Nullable net.minecraft.entity.player.PlayerEntity player) {
            return allowed;
        }
    }

    private enum TestItemCooldowns implements StrawShopPurchaseFlow.ItemCooldowns {
        READY(false),
        COOLING_DOWN(true);

        private final boolean coolingDown;

        TestItemCooldowns(boolean coolingDown) {
            this.coolingDown = coolingDown;
        }

        @Override
        public boolean isCoolingDown(
                @org.jetbrains.annotations.Nullable net.minecraft.entity.player.PlayerEntity player,
                ShopEntry entry
        ) {
            return coolingDown;
        }
    }
}
