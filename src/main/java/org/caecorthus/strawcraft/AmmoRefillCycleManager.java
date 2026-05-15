package org.caecorthus.strawcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AmmoRefillCycleManager {
    private final Map<UUID, CycleState> cyclesByGun = new HashMap<>();

    public Optional<AmmoGrant> observeGun(
            UUID holderUuid,
            UUID gunCycleId,
            GunAmmoFaction faction,
            TaczGunProfile profile,
            int currentAmmo,
            long nowTick
    ) {
        CycleState state = cyclesByGun.get(gunCycleId);
        if (state instanceof Cooldown cooldown) {
            return observeCooldown(holderUuid, gunCycleId, faction, profile, currentAmmo, nowTick, cooldown);
        }
        if (state instanceof WaitingForReload waitingForReload) {
            return observeWaitingForReload(holderUuid, gunCycleId, faction, profile, currentAmmo, nowTick, waitingForReload);
        }

        startCooldownIfLow(holderUuid, gunCycleId, faction, profile, currentAmmo, nowTick);
        return Optional.empty();
    }

    public void retainObservedGuns(Set<UUID> observedGunCycleIds) {
        cyclesByGun.keySet().removeIf(gunCycleId -> !observedGunCycleIds.contains(gunCycleId));
    }

    public void clearHolder(UUID holderUuid) {
        cyclesByGun.values().removeIf(state -> state.holderUuid().equals(holderUuid));
    }

    public void clearAll() {
        cyclesByGun.clear();
    }

    Optional<Long> deadlineTickFor(UUID gunCycleId) {
        CycleState state = cyclesByGun.get(gunCycleId);
        if (state instanceof Cooldown cooldown) {
            return Optional.of(cooldown.deadlineTick());
        }
        return Optional.empty();
    }

    private Optional<AmmoGrant> observeCooldown(
            UUID holderUuid,
            UUID gunCycleId,
            GunAmmoFaction faction,
            TaczGunProfile profile,
            int currentAmmo,
            long nowTick,
            Cooldown cooldown
    ) {
        if (!cooldown.sameHolderAndFaction(holderUuid, faction)) {
            cyclesByGun.remove(gunCycleId);
            startCooldownIfLow(holderUuid, gunCycleId, faction, profile, currentAmmo, nowTick);
            return Optional.empty();
        }
        if (!profile.isLowAmmo(currentAmmo)) {
            cyclesByGun.remove(gunCycleId);
            return Optional.empty();
        }
        if (nowTick < cooldown.deadlineTick()) {
            return Optional.empty();
        }

        // Grant ammo to the inventory, then wait until the gun's loaded ammo rises.
        // Otherwise a player could ignore reload and repeatedly farm the same low-ammo gun.
        int ammoCount = profile.missingAmmo(currentAmmo);
        cyclesByGun.put(gunCycleId, new WaitingForReload(holderUuid, faction, currentAmmo));
        if (ammoCount <= 0) {
            return Optional.empty();
        }
        return Optional.of(new AmmoGrant(holderUuid, gunCycleId, profile, ammoCount));
    }

    private Optional<AmmoGrant> observeWaitingForReload(
            UUID holderUuid,
            UUID gunCycleId,
            GunAmmoFaction faction,
            TaczGunProfile profile,
            int currentAmmo,
            long nowTick,
            WaitingForReload waitingForReload
    ) {
        if (currentAmmo <= waitingForReload.ammoAtGrant()) {
            cyclesByGun.put(gunCycleId, new WaitingForReload(holderUuid, faction, waitingForReload.ammoAtGrant()));
            return Optional.empty();
        }

        // A higher loaded-ammo count means TACZ has accepted a reload, so this gun may
        // start a fresh timer if it is still at the low-ammo threshold afterward.
        cyclesByGun.remove(gunCycleId);
        startCooldownIfLow(holderUuid, gunCycleId, faction, profile, currentAmmo, nowTick);
        return Optional.empty();
    }

    private void startCooldownIfLow(
            UUID holderUuid,
            UUID gunCycleId,
            GunAmmoFaction faction,
            TaczGunProfile profile,
            int currentAmmo,
            long nowTick
    ) {
        if (profile.isLowAmmo(currentAmmo)) {
            cyclesByGun.put(gunCycleId, new Cooldown(holderUuid, faction, nowTick + faction.refillDelayTicks()));
        }
    }

    private sealed interface CycleState permits Cooldown, WaitingForReload {
        UUID holderUuid();
    }

    private record Cooldown(UUID holderUuid, GunAmmoFaction faction, long deadlineTick) implements CycleState {
        boolean sameHolderAndFaction(UUID otherHolderUuid, GunAmmoFaction otherFaction) {
            return holderUuid.equals(otherHolderUuid) && faction == otherFaction;
        }
    }

    private record WaitingForReload(UUID holderUuid, GunAmmoFaction faction, int ammoAtGrant) implements CycleState {
    }

    public record AmmoGrant(UUID holderUuid, UUID gunCycleId, TaczGunProfile profile, int ammoCount) {
    }
}
