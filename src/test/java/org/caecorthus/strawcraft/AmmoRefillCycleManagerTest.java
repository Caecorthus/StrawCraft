package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmmoRefillCycleManagerTest {
    private static final TaczGunProfile P320 = TaczGunProfiles.profileFor(Identifier.of("tacz", "p320")).orElseThrow();

    @Test
    void lowAmmoStartsFactionTimerAndGrantsMissingAmmoAtDeadline() {
        AmmoRefillCycleManager manager = new AmmoRefillCycleManager();
        UUID holder = UUID.randomUUID();
        UUID gun = UUID.randomUUID();

        assertTrue(manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 4, 0).isEmpty());
        Optional<AmmoRefillCycleManager.AmmoGrant> grant =
                manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 4, GunAmmoFaction.POLICE.refillDelayTicks());

        assertEquals(8, grant.orElseThrow().ammoCount());
        assertEquals(holder, grant.orElseThrow().holderUuid());
        assertEquals(gun, grant.orElseThrow().gunCycleId());
    }

    @Test
    void waitsForReloadAfterGrantBeforeStartingAnotherTimer() {
        AmmoRefillCycleManager manager = new AmmoRefillCycleManager();
        UUID holder = UUID.randomUUID();
        UUID gun = UUID.randomUUID();

        manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 4, 0);
        manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 4, GunAmmoFaction.POLICE.refillDelayTicks());

        assertTrue(manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 4, GunAmmoFaction.POLICE.refillDelayTicks() + 1).isEmpty());
        assertTrue(manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 5, GunAmmoFaction.POLICE.refillDelayTicks() + 2).isEmpty());
        assertTrue(manager.observeGun(holder, gun, GunAmmoFaction.POLICE, P320, 4, GunAmmoFaction.POLICE.refillDelayTicks() + 3).isEmpty());
        assertEquals(GunAmmoFaction.POLICE.refillDelayTicks() * 2 + 3, manager.deadlineTickFor(gun).orElseThrow());
    }

    @Test
    void holderChangeCancelsActiveTimerAndRestartsForNewFaction() {
        AmmoRefillCycleManager manager = new AmmoRefillCycleManager();
        UUID killer = UUID.randomUUID();
        UUID civilian = UUID.randomUUID();
        UUID gun = UUID.randomUUID();

        manager.observeGun(killer, gun, GunAmmoFaction.KILLER, P320, 4, 0);
        manager.observeGun(civilian, gun, GunAmmoFaction.CIVILIAN, P320, 4, 100);

        assertEquals(100 + GunAmmoFaction.CIVILIAN.refillDelayTicks(), manager.deadlineTickFor(gun).orElseThrow());
    }

    @Test
    void absentGunsAndDeadPlayersClearTheirCycleState() {
        AmmoRefillCycleManager manager = new AmmoRefillCycleManager();
        UUID holder = UUID.randomUUID();
        UUID gun = UUID.randomUUID();

        manager.observeGun(holder, gun, GunAmmoFaction.KILLER, P320, 4, 0);
        manager.retainObservedGuns(Set.of());
        assertTrue(manager.deadlineTickFor(gun).isEmpty());

        manager.observeGun(holder, gun, GunAmmoFaction.KILLER, P320, 4, 0);
        manager.clearHolder(holder);
        assertTrue(manager.deadlineTickFor(gun).isEmpty());
    }
}
