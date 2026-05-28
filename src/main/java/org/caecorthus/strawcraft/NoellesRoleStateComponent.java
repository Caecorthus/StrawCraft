package org.caecorthus.strawcraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

public final class NoellesRoleStateComponent implements AutoSyncedComponent {
    public static final ComponentKey<NoellesRoleStateComponent> KEY =
            ComponentRegistry.getOrCreate(StrawCraft.id("noelles_role_state"), NoellesRoleStateComponent.class);

    private final PlayerEntity player;
    private final NoellesRoleState state = new NoellesRoleState();

    public NoellesRoleStateComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        // Shared per-player Noelles state stays generic; individual role mechanics define their own keys.
        // 这个共享玩家状态只保留通用键值；具体职业机制之后再定义自己的 key。
        state.reset();
        sync();
    }

    public boolean tryBeginAbilityCooldown(String abilityId, long now, int cooldownTicks) {
        boolean started = state.tryBeginAbilityCooldown(abilityId, now, cooldownTicks);
        if (started) {
            sync();
        }
        return started;
    }

    public boolean isAbilityOnCooldown(String abilityId, long now) {
        return state.isAbilityOnCooldown(abilityId, now);
    }

    public int getRemainingAbilityCooldown(String abilityId, long now) {
        return state.getRemainingAbilityCooldown(abilityId, now);
    }

    public void clearAbilityCooldown(String abilityId) {
        state.clearAbilityCooldown(abilityId);
        sync();
    }

    public void setFlag(String flag, boolean value) {
        state.setFlag(flag, value);
        sync();
    }

    public boolean hasFlag(String flag) {
        return state.hasFlag(flag);
    }

    public void setTimestamp(String key, long tick) {
        state.setTimestamp(key, tick);
        sync();
    }

    public OptionalLong getTimestamp(String key) {
        return state.getTimestamp(key);
    }

    public void setCounter(String key, int value) {
        state.setCounter(key, value);
        sync();
    }

    public int getCounter(String key) {
        return state.getCounter(key);
    }

    public int incrementCounter(String key) {
        int value = state.incrementCounter(key);
        sync();
        return value;
    }

    public boolean addUuidToSet(String key, UUID uuid) {
        boolean added = state.addUuidToSet(key, uuid);
        if (added) {
            sync();
        }
        return added;
    }

    public boolean removeUuidFromSet(String key, UUID uuid) {
        boolean removed = state.removeUuidFromSet(key, uuid);
        if (removed) {
            sync();
        }
        return removed;
    }

    public boolean uuidSetContains(String key, UUID uuid) {
        return state.uuidSetContains(key, uuid);
    }

    public Set<UUID> uuidSet(String key) {
        return state.uuidSet(key);
    }

    public void clearUuidSet(String key) {
        state.clearUuidSet(key);
        sync();
    }

    public void setReporterMarkedTarget(UUID targetUuid) {
        state.setReporterMarkedTarget(targetUuid);
        sync();
    }

    public java.util.Optional<UUID> reporterMarkedTarget() {
        return state.reporterMarkedTarget();
    }

    public void clearReporterMarkedTarget() {
        state.clearReporterMarkedTarget();
        sync();
    }

    public void setVoodooBondedTarget(UUID targetUuid) {
        state.setVoodooBondedTarget(targetUuid);
        sync();
    }

    public java.util.Optional<UUID> voodooBondedTarget() {
        return state.voodooBondedTarget();
    }

    public void clearVoodooBondedTarget() {
        state.clearVoodooBondedTarget();
        sync();
    }

    public void setPathogenInfectedBy(UUID pathogenUuid) {
        state.setPathogenInfectedBy(pathogenUuid);
        sync();
    }

    public java.util.Optional<UUID> pathogenInfectedBy() {
        return state.pathogenInfectedBy();
    }

    public void clearPathogenInfection() {
        state.clearPathogenInfection();
        sync();
    }

    public boolean trackDemonHunterFrenziedPlayer(UUID targetUuid) {
        boolean added = state.trackDemonHunterFrenziedPlayer(targetUuid);
        if (added) {
            sync();
        }
        return added;
    }

    public boolean untrackDemonHunterFrenziedPlayer(UUID targetUuid) {
        boolean removed = state.untrackDemonHunterFrenziedPlayer(targetUuid);
        if (removed) {
            sync();
        }
        return removed;
    }

    public boolean hasDemonHunterFrenziedPlayer(UUID targetUuid) {
        return state.hasDemonHunterFrenziedPlayer(targetUuid);
    }

    public Set<UUID> demonHunterFrenziedPlayers() {
        return state.demonHunterFrenziedPlayers();
    }

    public void clearDemonHunterFrenziedPlayers() {
        state.clearDemonHunterFrenziedPlayers();
        sync();
    }

    public void recordNeutralWinClaim(NoellesRoleState.NeutralWinClaim claim) {
        state.recordNeutralWinClaim(claim);
        sync();
    }

    public void clearNeutralWinClaim(net.minecraft.util.Identifier roleId) {
        state.clearNeutralWinClaim(roleId);
        sync();
    }

    public java.util.Optional<NoellesRoleState.NeutralWinClaim> neutralWinClaim(net.minecraft.util.Identifier roleId) {
        return state.neutralWinClaim(roleId);
    }

    public java.util.Set<NoellesRoleState.NeutralWinClaim> neutralWinClaims() {
        return state.neutralWinClaims();
    }

    public void setTimedBomb(NoellesRoleState.TimedBomb bomb) {
        state.setTimedBomb(bomb);
        sync();
    }

    public java.util.Optional<NoellesRoleState.TimedBomb> timedBomb() {
        return state.timedBomb();
    }

    public void clearTimedBomb() {
        state.clearTimedBomb();
        sync();
    }

    public void setRecallerRecallPoint(NoellesRoleState.RecallPoint recallPoint) {
        state.setRecallerRecallPoint(recallPoint);
        sync();
    }

    public java.util.Optional<NoellesRoleState.RecallPoint> recallerRecallPoint() {
        return state.recallerRecallPoint();
    }

    public void clearRecallerRecallPoint() {
        state.clearRecallerRecallPoint();
        sync();
    }

    public void sync() {
        if (player != null && !player.getWorld().isClient()) {
            KEY.sync(player);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        state.readFromNbt(nbt);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        state.writeToNbt(nbt);
    }
}
