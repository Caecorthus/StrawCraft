package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TaczAmmoRefillTimers {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaczAmmoRefillTimers.class);
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final AmmoRefillCycleManager CYCLE_MANAGER = new AmmoRefillCycleManager();
    private static final Set<Identifier> WARNED_AMBIGUOUS_ROLE_IDS = new HashSet<>();
    private static long serverTicks;

    private TaczAmmoRefillTimers() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TaczAmmoRefillTimers::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CYCLE_MANAGER.clearAll();
            WARNED_AMBIGUOUS_ROLE_IDS.clear();
            serverTicks = 0;
        });
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        CYCLE_MANAGER.clearHolder(player.getUuid());
    }

    private static void tickServer(MinecraftServer server) {
        serverTicks++;
        if (serverTicks % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        Set<UUID> observedGunCycleIds = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            scanPlayer(player, observedGunCycleIds);
        }
        CYCLE_MANAGER.retainObservedGuns(observedGunCycleIds);
    }

    private static void scanPlayer(ServerPlayerEntity player, Set<UUID> observedGunCycleIds) {
        if (!player.isAlive()) {
            CYCLE_MANAGER.clearHolder(player.getUuid());
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning() || game.isPlayerDead(player.getUuid())) {
            CYCLE_MANAGER.clearHolder(player.getUuid());
            return;
        }

        Optional<GunAmmoFaction> faction = resolveFaction(game, player);
        if (faction.isEmpty()) {
            CYCLE_MANAGER.clearHolder(player.getUuid());
            return;
        }

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            observeStack(player, stack, faction.get(), observedGunCycleIds);
        }
    }

    private static Optional<GunAmmoFaction> resolveFaction(GameWorldComponent game, ServerPlayerEntity player) {
        Role role = game.getRole(player);
        if (GunAmmoFactionTags.isAmbiguous(role) && WARNED_AMBIGUOUS_ROLE_IDS.add(role.identifier())) {
            LOGGER.warn("Wathe role {} matches multiple StrawCraft gun ammo faction tags; automatic ammo refill is disabled for it.", role.identifier());
        }
        return GunAmmoFactionTags.resolve(role);
    }

    private static void observeStack(
            ServerPlayerEntity holder,
            ItemStack stack,
            GunAmmoFaction faction,
            Set<UUID> observedGunCycleIds
    ) {
        Optional<TaczGunProfile> profile = TaczGunStacks.getGunId(stack).flatMap(TaczGunProfiles::profileFor);
        if (profile.isEmpty()) {
            return;
        }

        int currentAmmo = TaczGunStacks.getCurrentAmmo(stack);
        Optional<UUID> existingCycleId = TaczGunStacks.getAmmoCycleId(stack);
        // Do not stamp every TACZ gun forever; only low-ammo guns enter StrawCraft's cycle tracking.
        if (existingCycleId.isEmpty() && !profile.get().isLowAmmo(currentAmmo)) {
            return;
        }

        UUID gunCycleId = existingCycleId.orElseGet(() -> TaczGunStacks.getOrCreateAmmoCycleId(stack));
        observedGunCycleIds.add(gunCycleId);
        CYCLE_MANAGER.observeGun(holder.getUuid(), gunCycleId, faction, profile.get(), currentAmmo, serverTicks)
                .ifPresent(grant -> giveAmmo(holder, grant));
    }

    private static void giveAmmo(ServerPlayerEntity player, AmmoRefillCycleManager.AmmoGrant grant) {
        ItemStack ammoStack = TaczGunStacks.createAmmoStack(grant.profile(), grant.ammoCount());
        if (ammoStack.isEmpty()) {
            return;
        }

        ItemStack remaining = ammoStack.copy();
        player.getInventory().insertStack(remaining);
        if (!remaining.isEmpty()) {
            player.dropItem(remaining, false, true);
        }
    }
}
