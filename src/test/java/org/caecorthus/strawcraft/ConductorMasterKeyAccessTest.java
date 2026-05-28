package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConductorMasterKeyAccessTest {
    @Test
    void conductorCarryingMasterKeyCanOpenLockedDoorsDuringRunningRound() {
        assertTrue(ConductorMasterKeyAccess.allowsLockedDoorAccess(
                StrawCraft.id("conductor"),
                true,
                List.of(StrawCraft.id("master_key"))
        ));
    }

    @Test
    void nonConductorCarryingMasterKeyCannotOpenLockedDoors() {
        assertFalse(ConductorMasterKeyAccess.allowsLockedDoorAccess(
                StrawCraft.id("detective"),
                true,
                List.of(StrawCraft.id("master_key"))
        ));
    }

    @Test
    void conductorWithoutMasterKeyCannotOpenLockedDoors() {
        assertFalse(ConductorMasterKeyAccess.allowsLockedDoorAccess(
                StrawCraft.id("conductor"),
                true,
                List.of(Identifier.ofVanilla("stick"))
        ));
    }

    @Test
    void nullRoleCannotOpenLockedDoorsEvenWithMasterKey() {
        assertFalse(ConductorMasterKeyAccess.allowsLockedDoorAccess(
                null,
                true,
                List.of(StrawCraft.id("master_key"))
        ));
    }

    @Test
    void nonRunningRoundCannotOpenLockedDoorsEvenWithConductorAndMasterKey() {
        assertFalse(ConductorMasterKeyAccess.allowsLockedDoorAccess(
                StrawCraft.id("conductor"),
                false,
                List.of(StrawCraft.id("master_key"))
        ));
    }

    @Test
    void emptyInventoryCannotOpenLockedDoors() {
        assertFalse(ConductorMasterKeyAccess.allowsLockedDoorAccess(
                StrawCraft.id("conductor"),
                true,
                List.of()
        ));
    }
}
