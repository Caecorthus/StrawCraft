package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaiterFeedingPolicyTest {
    @Test
    void waiterCanServeMatchingOpenFoodOrDrinkTasks() {
        Role waiter = role("waiter");

        assertEquals(Optional.of(WaiterFeedingPolicy.Service.EAT), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.FOOD, true, false
        )));
        assertEquals(Optional.of(WaiterFeedingPolicy.Service.DRINK), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.DRINK, false, true
        )));
    }

    @Test
    void waiterServiceTrayCanSatisfyEitherTaskAndPrefersFoodWhenBothAreOpen() {
        Role waiter = role("waiter");

        assertEquals(Optional.of(WaiterFeedingPolicy.Service.EAT), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.SERVICE, true, true
        )));
        assertEquals(Optional.of(WaiterFeedingPolicy.Service.DRINK), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.SERVICE, false, true
        )));
    }

    @Test
    void serviceRequiresWaiterRoleLivingPlayersDifferentTargetValidItemAndOpenNeed() {
        Role waiter = role("waiter");

        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                role("bartender"), true, true, false, WaiterFeedingPolicy.ServiceKind.SERVICE, true, false
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, false, true, false, WaiterFeedingPolicy.ServiceKind.SERVICE, true, false
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, true, false, false, WaiterFeedingPolicy.ServiceKind.SERVICE, true, false
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, true, WaiterFeedingPolicy.ServiceKind.SERVICE, true, false
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.INVALID, true, false
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.FOOD, false, true
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.DRINK, true, false
        )));
        assertEquals(Optional.empty(), WaiterFeedingPolicy.chooseService(input(
                waiter, true, true, false, WaiterFeedingPolicy.ServiceKind.SERVICE, false, false
        )));
    }

    private static WaiterFeedingPolicy.Input input(
            Role role,
            boolean actorAlive,
            boolean targetAlive,
            boolean samePlayer,
            WaiterFeedingPolicy.ServiceKind serviceKind,
            boolean targetNeedsFood,
            boolean targetNeedsDrink
    ) {
        return new WaiterFeedingPolicy.Input(role, actorAlive, targetAlive, samePlayer, serviceKind, targetNeedsFood, targetNeedsDrink);
    }

    private static Role role(String path) {
        return new Role(StrawCraft.id(path), 0xFFFFFF, true, false, Role.MoodType.REAL, 200, false);
    }
}
