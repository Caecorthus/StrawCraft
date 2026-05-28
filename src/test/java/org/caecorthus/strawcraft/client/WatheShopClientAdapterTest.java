package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.util.ShopEntry;
import org.caecorthus.strawcraft.PlayerShopCatalog;
import org.caecorthus.strawcraft.StrawShopEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class WatheShopClientAdapterTest {
    @Test
    void createsPureSnapshotFromWatheEntriesAndShopState() {
        ShopEntry revolver = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);
        ShopEntry knife = new ShopEntry(null, 100, ShopEntry.Type.WEAPON);

        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                List.of(revolver, knife),
                new FakeShopState()
        );

        assertEquals(OptionalInt.of(450), snapshot.balance());
        assertEquals(2, snapshot.entries().size());
        assertSame(revolver, snapshot.entries().get(0));
        assertSame(knife, snapshot.entries().get(1));

        ShopEntryViewState revolverState = snapshot.entryStates().get(0);
        assertTrue(revolverState.active());
        assertTrue(revolverState.cooldownStatus().isEmpty());
        assertTrue(revolverState.stockStatus().isEmpty());

        ShopEntryViewState knifeState = snapshot.entryStates().get(1);
        assertTrue(knifeState.active());
        assertTrue(knifeState.status().isEmpty());
    }

    @Test
    void missingShopStateStillKeepsEntriesVisibleWithPrices() {
        ShopEntry revolver = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);

        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(List.of(revolver), null);

        assertTrue(snapshot.balance().isEmpty());
        assertEquals(1, snapshot.entries().size());
        assertTrue(snapshot.entryStates().get(0).active());
        assertEquals("300", snapshot.entryStates().get(0).priceText());
    }

    @Test
    void entryStatesCarryOriginalPurchaseIndexAndPresentationFieldsInWatheOrder() {
        ShopEntry tool = new ShopEntry(null, 100, ShopEntry.Type.TOOL);
        ShopEntry weapon = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);
        ShopEntry poison = new ShopEntry(null, 250, ShopEntry.Type.POISON);

        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                List.of(tool, weapon, poison),
                new FakeShopState()
        );

        Method wathePurchaseIndex = viewStateAccessor("wathePurchaseIndex");
        Method type = viewStateAccessor("type");
        Method displayStack = viewStateAccessor("displayStack");

        assertEquals(List.of(0, 1, 2), snapshot.entryStates().stream()
                .map(state -> invokeInt(wathePurchaseIndex, state))
                .toList());
        assertEquals(List.of("100", "300", "250"), snapshot.entryStates().stream()
                .map(ShopEntryViewState::priceText)
                .toList());
        assertEquals(List.of(ShopEntry.Type.TOOL, ShopEntry.Type.WEAPON, ShopEntry.Type.POISON), snapshot.entryStates().stream()
                .map(state -> invoke(type, state))
                .toList());
        snapshot.entryStates().forEach(state -> assertNull(invoke(displayStack, state)));
    }

    @Test
    void buyUsesVisibleItemStoredWathePurchaseIndexWhenVisibleOrderDiffers() {
        ShopEntry tool = new ShopEntry(null, 100, ShopEntry.Type.TOOL);
        ShopEntry weapon = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);
        ShopEntry poison = new ShopEntry(null, 250, ShopEntry.Type.POISON);

        WatheShopClientAdapter.ShopSnapshot watheOrder = WatheShopClientAdapter.snapshotFrom(
                List.of(tool, weapon, poison),
                new FakeShopState()
        );
        WatheShopClientAdapter.ShopSnapshot visibleOrder = new WatheShopClientAdapter.ShopSnapshot(
                List.of(weapon, tool, poison),
                watheOrder.balance(),
                List.of(
                        watheOrder.entryStates().get(1),
                        watheOrder.entryStates().get(0),
                        watheOrder.entryStates().get(2)
                ),
                List.of(
                        watheOrder.entryKeys().get(1),
                        watheOrder.entryKeys().get(0),
                        watheOrder.entryKeys().get(2)
                )
        );
        List<Integer> sentIndices = new ArrayList<>();

        new WatheShopClientAdapter(visibleOrder, sentIndices::add).buy(0);

        assertEquals(List.of(1), sentIndices);
    }

    @Test
    void snapshotFromPlayerPresentationUsesWathePurchaseIndexInsteadOfVisibleIndex() {
        ShopEntry visibleGrenade = new ShopEntry(null, 350, ShopEntry.Type.WEAPON);
        PlayerShopCatalog.Presentation presentation = new PlayerShopCatalog.Presentation(List.of(
                new PlayerShopCatalog.VisibleEntry(1, visibleGrenade)
        ));

        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                presentation,
                new FakeShopState()
        );

        assertEquals(List.of(visibleGrenade), snapshot.entries());
        assertEquals(1, snapshot.entryStates().getFirst().wathePurchaseIndex());
    }

    @Test
    void buyKeepsMatchingVisibleAndWatheOrderBehavior() {
        ShopEntry tool = new ShopEntry(null, 100, ShopEntry.Type.TOOL);
        ShopEntry weapon = new ShopEntry(null, 300, ShopEntry.Type.WEAPON);
        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                List.of(tool, weapon),
                new FakeShopState()
        );
        List<Integer> sentIndices = new ArrayList<>();

        new WatheShopClientAdapter(snapshot, sentIndices::add).buy(0);

        assertEquals(List.of(0), sentIndices);
    }

    @Test
    void buyIgnoresNegativeVisibleIndexWithoutSendingPurchase() {
        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                List.of(new ShopEntry(null, 100, ShopEntry.Type.TOOL)),
                new FakeShopState()
        );
        List<Integer> sentIndices = new ArrayList<>();
        WatheShopClientAdapter adapter = new WatheShopClientAdapter(snapshot, sentIndices::add);

        assertDoesNotThrow(() -> adapter.buy(-1));

        assertTrue(sentIndices.isEmpty());
    }

    @Test
    void buyIgnoresTooLargeVisibleIndexWithoutSendingPurchase() {
        WatheShopClientAdapter.ShopSnapshot snapshot = WatheShopClientAdapter.snapshotFrom(
                List.of(new ShopEntry(null, 100, ShopEntry.Type.TOOL)),
                new FakeShopState()
        );
        List<Integer> sentIndices = new ArrayList<>();
        WatheShopClientAdapter adapter = new WatheShopClientAdapter(snapshot, sentIndices::add);

        assertDoesNotThrow(() -> adapter.buy(1));

        assertTrue(sentIndices.isEmpty());
    }

    @Test
    void buyBeforeSnapshotIgnoresVisibleIndexWithoutSendingPurchase() {
        List<Integer> sentIndices = new ArrayList<>();
        WatheShopClientAdapter adapter = new WatheShopClientAdapter(null, sentIndices::add);

        assertDoesNotThrow(() -> adapter.buy(0));

        assertTrue(sentIndices.isEmpty());
    }

    @Test
    void entryKeysIgnoreShopStateAndRepresentCustomData() {
        ShopEntry p320 = new StrawShopEntry("p320", null, null, 300, ShopEntry.Type.WEAPON, 40, 20, 2);
        ShopEntry p320OnCooldown = new StrawShopEntry("p320", null, null, 300, ShopEntry.Type.WEAPON, 40, 20, 2);

        WatheShopClientAdapter.ShopSnapshot available = WatheShopClientAdapter.snapshotFrom(
                List.of(p320),
                new FakeShopState()
        );
        WatheShopClientAdapter.ShopSnapshot cooldown = WatheShopClientAdapter.snapshotFrom(
                List.of(p320OnCooldown),
                new FakeShopState()
        );

        assertEquals(available.entryKeys(), cooldown.entryKeys());
        assertFalse(stackKey("GunId=tacz:p320").equals(stackKey("GunId=tacz:rhino357")));
    }

    private static final class FakeShopState implements WatheShopClientAdapter.ShopState {
        @Override
        public OptionalInt balance() {
            return OptionalInt.of(450);
        }

        @Override
        public ShopEntryViewState.Snapshot snapshotFor(ShopEntry entry) {
            return new ShopEntryViewState.Snapshot(
                    entry.price(),
                    entry instanceof StrawShopEntry,
                    entry instanceof StrawShopEntry ? 300 : 0,
                    entry instanceof StrawShopEntry ? 2 : -1,
                    entry instanceof StrawShopEntry ? 1 : -1,
                    true
            );
        }
    }

    private static WatheShopClientAdapter.StackKey stackKey(String customData) {
        return new WatheShopClientAdapter.StackKey("tacz:modern_kinetic_gun", 1, "Modern Kinetic Gun", customData);
    }

    private static Method viewStateAccessor(String name) {
        try {
            return ShopEntryViewState.class.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return fail("ShopEntryViewState should expose " + name + "() so UI code can render and buy without reading ShopEntry directly");
        }
    }

    private static Object invoke(Method method, ShopEntryViewState state) {
        try {
            return method.invoke(state);
        } catch (IllegalAccessException exception) {
            return fail(method.getName() + "() should be public on ShopEntryViewState");
        } catch (InvocationTargetException exception) {
            return fail(method.getName() + "() threw " + exception.getCause());
        }
    }

    private static int invokeInt(Method method, ShopEntryViewState state) {
        return (Integer) invoke(method, state);
    }
}
