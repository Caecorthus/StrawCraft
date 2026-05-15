package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmmoRefillCycleManagerTest {
    private static final TaczGunProfile P320 = TaczGunProfiles.P320;

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

    @Test
    void scanObservesLowAmmoTaczGunStacksAndGrantsMissingAmmoAtDeadline() {
        AmmoRefillCycleManager manager = new AmmoRefillCycleManager();
        UUID holder = UUID.randomUUID();
        AmmoRefillCycleManager.ObservedGunStack stack = gunStack(P320, 4, Optional.empty());

        manager.beginScan();
        AmmoRefillCycleManager.StackObservation firstObservation = manager.observeStack(holder, stack, GunAmmoFaction.POLICE, 0);
        manager.finishScan();

        UUID gunCycleId = firstObservation.createdAmmoCycleId().orElseThrow();
        manager.beginScan();
        AmmoRefillCycleManager.StackObservation secondObservation =
                manager.observeStack(holder, gunStack(P320, 4, Optional.of(gunCycleId)), GunAmmoFaction.POLICE, GunAmmoFaction.POLICE.refillDelayTicks());
        manager.finishScan();

        AmmoRefillCycleManager.AmmoGrant grant = secondObservation.ammoGrant().orElseThrow();
        assertEquals(8, grant.ammoCount());
        assertEquals(gunCycleId, grant.gunCycleId());
    }

    @Test
    void scanDoesNotStampFullAmmoTaczGunStacks() {
        AmmoRefillCycleManager manager = new AmmoRefillCycleManager();

        manager.beginScan();
        AmmoRefillCycleManager.StackObservation observation =
                manager.observeStack(UUID.randomUUID(), gunStack(P320, 12, Optional.empty()), GunAmmoFaction.POLICE, 0);
        manager.finishScan();

        assertTrue(observation.createdAmmoCycleId().isEmpty());
        assertTrue(observation.ammoGrant().isEmpty());
    }

    private static AmmoRefillCycleManager.ObservedGunStack gunStack(
            TaczGunProfile profile,
            int currentAmmo,
            Optional<UUID> ammoCycleId
    ) {
        return new AmmoRefillCycleManager.ObservedGunStack(profile, currentAmmo, ammoCycleId);
    }
}
