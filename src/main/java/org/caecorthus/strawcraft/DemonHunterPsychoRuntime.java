package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DemonHunterPsychoRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private DemonHunterPsychoRuntime() {
    }

    public static void register() {
        REGISTERED.compareAndSet(false, true);
    }

    public static void onLoudPsychoStarted(PlayerEntity frenziedPlayer) {
        if (!(frenziedPlayer instanceof ServerPlayerEntity frenzied)) {
            return;
        }
        ServerWorld world = frenzied.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity demonHunter : world.getPlayers()) {
            handlePsychoStartedForDemonHunter(game, demonHunter, frenzied);
        }
    }

    public static void onLoudPsychoStopped(PlayerEntity frenziedPlayer) {
        if (!(frenziedPlayer instanceof ServerPlayerEntity frenzied)) {
            return;
        }
        ServerWorld world = frenzied.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity demonHunter : world.getPlayers()) {
            if (!isDemonHunter(game.getRole(demonHunter))) {
                continue;
            }
            NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(demonHunter);
            state.untrackDemonHunterFrenziedPlayer(frenzied.getUuid());
            if (state.demonHunterFrenziedPlayers().isEmpty()) {
                removeEmptyDemonHunterPistols(demonHunter);
            }
        }
    }

    private static void handlePsychoStartedForDemonHunter(
            GameWorldComponent game,
            ServerPlayerEntity demonHunter,
            ServerPlayerEntity frenzied
    ) {
        Optional<ItemStack> existingPistol = findDemonHunterPistol(demonHunter);
        DemonHunterPsychoPolicy.PsychoStartResult result = DemonHunterPsychoPolicy.onPsychoStarted(
                new DemonHunterPsychoPolicy.PsychoStartInput(
                        game.isRunning(),
                        true,
                        isDemonHunter(game.getRole(demonHunter)),
                        GameFunctions.isPlayerAliveAndSurvival(demonHunter),
                        GameFunctions.isPlayerAliveAndSurvival(frenzied),
                        existingPistol.isPresent(),
                        ownsOtherGun(demonHunter)
                )
        );
        if (!result.tracksFrenziedPlayer()) {
            return;
        }

        NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(demonHunter);
        state.trackDemonHunterFrenziedPlayer(frenzied.getUuid());
        if (result == DemonHunterPsychoPolicy.PsychoStartResult.GIVE_PISTOL) {
            givePistol(demonHunter, result.bulletsToAdd());
        } else if (result == DemonHunterPsychoPolicy.PsychoStartResult.ADD_BULLETS) {
            existingPistol.ifPresent(stack -> DemonHunterPistolItem.addBullets(stack, result.bulletsToAdd()));
        }
    }

    private static boolean isDemonHunter(Role role) {
        return StrawRoleMeaning.receivesDemonHunterPsychoResponse(role);
    }

    private static void givePistol(ServerPlayerEntity player, int bullets) {
        ItemStack pistol = DemonHunterPistolItem.createStack(bullets);
        ItemStack remaining = pistol.copy();
        player.getInventory().insertStack(remaining);
        if (!remaining.isEmpty()) {
            player.dropItem(remaining, false, true);
        }
    }

    private static Optional<ItemStack> findDemonHunterPistol(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (DemonHunterPistolItem.isDemonHunterPistol(stack)) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    private static boolean ownsOtherGun(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (DemonHunterPistolItem.isDemonHunterPistol(stack)) {
                continue;
            }
            if (WeaponBalance.isDisabledWatheGun(stack)
                    || stack.isOf(WatheItems.REVOLVER)
                    || stack.isOf(WatheItems.DERRINGER)
                    || TaczGunStacks.getGunId(stack).isPresent()) {
                return true;
            }
        }
        return false;
    }

    static void removeEmptyDemonHunterPistols(ServerPlayerEntity player) {
        player.getInventory().remove(
                DemonHunterPistolItem::isDemonHunterPistol,
                Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput()
        );
    }
}
