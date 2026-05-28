package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.api.StrawShopEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BomberTimedBombRuntime {
    static final String TIMED_BOMB_ENTRY_ID = "timed_bomb";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int TIMED_BOMB_PRICE = 300;

    private BomberTimedBombRuntime() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        StrawShopEvents.BUILD_ENTRIES.register(BomberTimedBombRuntime::addTimedBombEntry);
        ServerTickEvents.END_SERVER_TICK.register(BomberTimedBombRuntime::tickServer);
    }

    static void addTimedBombEntry(StrawShopEvents.ShopContext context) {
        context.replaceEntries(withTimedBombEntry(context.getEntries()));
    }

    static List<ShopEntry> withTimedBombEntry(List<ShopEntry> entries) {
        if (entries.stream().anyMatch(BomberTimedBombRuntime::isTimedBombEntry)) {
            return List.copyOf(entries);
        }
        List<ShopEntry> nextEntries = new ArrayList<>(entries.size() + 1);
        nextEntries.addAll(entries);
        nextEntries.add(timedBombEntry());
        return List.copyOf(nextEntries);
    }

    static boolean isTimedBombEntry(ShopEntry entry) {
        return TIMED_BOMB_ENTRY_ID.equals(StrawShopEntry.idFor(entry));
    }

    static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        long now = world.getTime();
        for (ServerPlayerEntity carrier : world.getPlayers()) {
            tickCarrier(world, game, carrier, now);
        }
    }

    private static void tickCarrier(ServerWorld world, GameWorldComponent game, ServerPlayerEntity carrier, long now) {
        NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(carrier);
        state.timedBomb().ifPresent(bomb -> resolveBomb(world, game, carrier, state, bomb, now));
    }

    private static void resolveBomb(
            ServerWorld world,
            GameWorldComponent game,
            ServerPlayerEntity carrier,
            NoellesRoleStateComponent state,
            NoellesRoleState.TimedBomb bomb,
            long now
    ) {
        BomberTimedBombPolicy.ExpiryResult result = BomberTimedBombPolicy.expiryResult(
                bomb,
                carrier.getUuid(),
                game.isRunning(),
                GameFunctions.isPlayerAliveAndSurvival(carrier),
                now
        );
        if (result == BomberTimedBombPolicy.ExpiryResult.WAIT || result == BomberTimedBombPolicy.ExpiryResult.NO_BOMB) {
            return;
        }

        state.clearTimedBomb();
        if (result == BomberTimedBombPolicy.ExpiryResult.KILL_CARRIER) {
            PlayerEntity owner = world.getPlayerByUuid(bomb.ownerUuid());
            if (owner == carrier) {
                owner = null;
            }
            GameFunctions.killPlayer(carrier, true, owner, StrawDeathReasons.BOMB);
        }
    }

    private static ShopEntry timedBombEntry() {
        ItemStack displayStack = timedBombStack();
        return new StrawShopEntry(
                TIMED_BOMB_ENTRY_ID,
                displayStack,
                displayStack,
                TIMED_BOMB_PRICE,
                ShopEntry.Type.WEAPON,
                0,
                0,
                -1
        );
    }

    private static ItemStack timedBombStack() {
        ItemStack stack = new ItemStack(StrawCraftItems.TIMED_BOMB);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.strawcraft." + TIMED_BOMB_ENTRY_ID));
        return stack;
    }
}
