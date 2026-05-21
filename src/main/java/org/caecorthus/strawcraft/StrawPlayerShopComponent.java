package org.caecorthus.strawcraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public final class StrawPlayerShopComponent implements AutoSyncedComponent {
    public static final ComponentKey<StrawPlayerShopComponent> KEY =
            ComponentRegistry.getOrCreate(StrawCraft.id("shop"), StrawPlayerShopComponent.class);

    private final PlayerEntity player;
    private final StrawPlayerShopState state = new StrawPlayerShopState();

    public StrawPlayerShopComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        state.reset();
        sync();
    }

    public void ensureEntry(StrawShopEntry entry, long now) {
        state.ensureEntry(entry, now);
    }

    public boolean canPurchase(StrawShopEntry entry, long now) {
        return state.canPurchase(entry, now);
    }

    public void recordPurchase(StrawShopEntry entry, long now) {
        state.recordPurchase(entry, now);
        sync();
    }

    public boolean isOnCooldown(String entryId, long now) {
        return state.isOnCooldown(entryId, now);
    }

    public int getRemainingCooldown(String entryId, long now) {
        return state.getRemainingCooldown(entryId, now);
    }

    public int getRemainingStock(String entryId) {
        return state.getRemainingStock(entryId);
    }

    public boolean isInStock(String entryId) {
        return state.isInStock(entryId);
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
