package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineerDoorRepairPolicyTest {
    @Test
    void allowsServerRunningAliveEngineerWithRepairToolCooldownAndOfficialDoorTarget() {
        assertTrue(EngineerDoorRepairPolicy.shouldHandle(validInput()));
    }

    @Test
    void deniesClientStoppedWrongRoleDeadMissingToolCooldownAndNonWatheDoorContexts() {
        assertBlocked(validInput().withClientWorld(true));
        assertBlocked(validInput().withRoundRunning(false));
        assertBlocked(validInput().withRole(noellesRole("conductor")));
        assertBlocked(validInput().withEngineerAlive(false));
        assertBlocked(validInput().withRepairToolInHand(false));
        assertBlocked(validInput().withCooldownReady(false));
        assertBlocked(validInput().withOfficialWatheSmallDoorTarget(false));
    }

    @Test
    void mapsDoorStateToRepairClearUnlockLockOrPassActions() {
        assertEquals(EngineerDoorRepairPolicy.DoorAction.PASS,
                EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(false, false, false, "")));
        assertEquals(EngineerDoorRepairPolicy.DoorAction.REPAIR_BLASTED,
                EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(true, true, true, "suite")));
        assertEquals(EngineerDoorRepairPolicy.DoorAction.CLEAR_JAMMED,
                EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(true, false, true, "suite")));
        assertEquals(EngineerDoorRepairPolicy.DoorAction.UNLOCK,
                EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(true, false, false, "suite")));
        assertEquals(EngineerDoorRepairPolicy.DoorAction.LOCK,
                EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(true, false, false, "")));
        assertEquals(EngineerDoorRepairPolicy.DoorAction.LOCK,
                EngineerDoorRepairPolicy.actionFor(new EngineerDoorRepairPolicy.DoorState(true, false, false, null)));
    }

    private static void assertBlocked(EngineerDoorRepairPolicy.InteractionInput input) {
        assertFalse(EngineerDoorRepairPolicy.shouldHandle(input));
    }

    private static EngineerDoorRepairPolicy.InteractionInput validInput() {
        return new EngineerDoorRepairPolicy.InteractionInput(
                false,
                true,
                noellesRole("engineer"),
                true,
                true,
                true,
                true
        );
    }

    private static Role noellesRole(String path) {
        return NoellesRoleCatalog.find(StrawCraft.id(path)).orElseThrow().watheRole();
    }
}
