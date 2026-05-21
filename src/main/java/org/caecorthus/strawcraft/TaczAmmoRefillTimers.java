package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class TaczAmmoRefillTimers {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaczAmmoRefillTimers.class);
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final AmmoRefillCycleManager CYCLE_MANAGER = new AmmoRefillCycleManager();
    private static final Set<String> WARNED_AMBIGUOUS_ROLES = new HashSet<>();
    private static long serverTicks;

    private TaczAmmoRefillTimers() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TaczAmmoRefillTimers::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CYCLE_MANAGER.clearAll();
            WARNED_AMBIGUOUS_ROLES.clear();
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

        CYCLE_MANAGER.beginScan();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            scanPlayer(player);
        }
        CYCLE_MANAGER.finishScan();
    }

    private static void scanPlayer(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!WatheRoundParticipantLifecycle.shouldTrackRuntimeState(player, game)) {
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
            observeStack(player, stack, faction.get());
        }
    }

    private static Optional<GunAmmoFaction> resolveFaction(GameWorldComponent game, ServerPlayerEntity player) {
        Role role = game.getRole(player);
        String roleDescription = StrawRoleMeaning.describeForLog(role);
        if (GunAmmoFactionTags.isAmbiguous(role) && WARNED_AMBIGUOUS_ROLES.add(roleDescription)) {
            LOGGER.warn("Wathe role {} matches multiple StrawCraft gun ammo faction tags; automatic ammo refill is disabled for it.", roleDescription);
        }
        return GunAmmoFactionTags.resolve(role);
    }

    private static void observeStack(ServerPlayerEntity player, ItemStack stack, GunAmmoFaction faction) {
        Optional<TaczGunProfile> profile = TaczGunStacks.getGunId(stack).flatMap(TaczGunProfiles::profileFor);
        if (profile.isEmpty()) {
            return;
        }

        AmmoRefillCycleManager.ObservedGunStack observedStack = new AmmoRefillCycleManager.ObservedGunStack(
                profile.get(),
                TaczGunStacks.getCurrentAmmo(stack),
                TaczGunStacks.getAmmoCycleId(stack)
        );
        AmmoRefillCycleManager.StackObservation observation =
                CYCLE_MANAGER.observeStack(player.getUuid(), observedStack, faction, serverTicks);
        observation.createdAmmoCycleId().ifPresent(cycleId -> TaczGunStacks.setAmmoCycleId(stack, cycleId));
        observation.ammoGrant().ifPresent(grant -> giveAmmo(player, grant));
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
