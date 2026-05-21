package org.caecorthus.strawcraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AmmoRefillCycleManager {
    private final Map<UUID, CycleState> cyclesByGun = new HashMap<>();
    private final Set<UUID> observedGunCycleIds = new HashSet<>();

    public void beginScan() {
        observedGunCycleIds.clear();
    }

    public void finishScan() {
        retainObservedGuns(observedGunCycleIds);
        observedGunCycleIds.clear();
    }

    public StackObservation observeStack(
            UUID holderUuid,
            ObservedGunStack stack,
            GunAmmoFaction faction,
            long nowTick
    ) {
        // Do not stamp every TACZ gun forever; only low-ammo guns enter StrawCraft's cycle tracking.
        // 不给每把 TACZ 枪永久打标；只有低弹药状态的枪才进入 StrawCraft 的补弹周期跟踪。
        if (stack.ammoCycleId().isEmpty() && !stack.profile().isLowAmmo(stack.currentAmmo())) {
            return StackObservation.empty();
        }

        UUID gunCycleId = stack.ammoCycleId().orElseGet(UUID::randomUUID);
        observedGunCycleIds.add(gunCycleId);
        Optional<AmmoGrant> grant = observeGun(holderUuid, gunCycleId, faction, stack.profile(), stack.currentAmmo(), nowTick);
        return new StackObservation(stack.ammoCycleId().isEmpty() ? Optional.of(gunCycleId) : Optional.empty(), grant);
    }

    Optional<AmmoGrant> observeGun(
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

    void retainObservedGuns(Set<UUID> observedGunCycleIds) {
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
        // 先把弹药发到物品栏，然后等枪内弹药数真的上涨。
        // 否则玩家可以不换弹，反复用同一把低弹药枪刷补给。
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
        // 枪内弹药数变高说明 TACZ 已经完成换弹；如果换完后仍然处于低弹药阈值，
        // 这把枪可以重新开始下一轮计时。
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

    record AmmoGrant(UUID holderUuid, UUID gunCycleId, TaczGunProfile profile, int ammoCount) {
    }

    record ObservedGunStack(TaczGunProfile profile, int currentAmmo, Optional<UUID> ammoCycleId) {
    }

    record StackObservation(Optional<UUID> createdAmmoCycleId, Optional<AmmoGrant> ammoGrant) {
        private static StackObservation empty() {
            return new StackObservation(Optional.empty(), Optional.empty());
        }
    }
}
