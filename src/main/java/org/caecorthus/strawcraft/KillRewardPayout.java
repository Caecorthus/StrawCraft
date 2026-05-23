package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class KillRewardPayout {
    private KillRewardPayout() {
    }

    public static void payoutVanillaDeath(
            ServerPlayerEntity victim,
            GameWorldComponent game,
            WatheDeathReasonTracker.DeathAttribution attribution
    ) {
        MinecraftServer server = victim.getServer();
        List<KillRewardPolicy.KillerParticipant> killerTeam = game.getAllKillerTeamPlayers().stream()
                .map(killerUuid -> new KillRewardPolicy.KillerParticipant(
                        killerUuid,
                        livingServerPlayer(server, killerUuid) != null
                ))
                .toList();

        List<KillRewardPolicy.Grant> grants = KillRewardPolicy.compute(
                new KillRewardPolicy.Context(
                        victim.getUuid(),
                        game.isInnocent(victim),
                        attribution.killerUuid().orElse(null),
                        attribution.indirect()
                ),
                killerTeam
        );
        apply(grants, recipientUuid -> accountFor(server, recipientUuid));
    }

    public static void apply(List<KillRewardPolicy.Grant> grants, Function<UUID, @Nullable Account> accounts) {
        Map<UUID, Integer> totals = new HashMap<>();
        for (KillRewardPolicy.Grant grant : grants) {
            if (grant.amount() <= 0) {
                continue;
            }
            totals.merge(grant.recipientUuid(), grant.amount(), Integer::sum);
        }

        totals.forEach((recipientUuid, amount) -> {
            Account account = accounts.apply(recipientUuid);
            if (account == null) {
                return;
            }
            account.setBalance(account.balance() + amount);
            account.sync();
        });
    }

    @Nullable
    private static Account accountFor(MinecraftServer server, UUID uuid) {
        // Main kill rewards may pay an online killer after Wathe has moved them out of survival.
        // 主击杀奖励可能在 Wathe 把杀手移出 survival 后发放，因此账户查询只要求在线。
        ServerPlayerEntity player = onlineServerPlayer(server, uuid);
        if (player == null) {
            return null;
        }
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        return new Account() {
            @Override
            public int balance() {
                return shop.balance;
            }

            @Override
            public void setBalance(int balance) {
                shop.balance = balance;
            }

            @Override
            public void sync() {
                shop.sync();
            }
        };
    }

    @Nullable
    private static ServerPlayerEntity livingServerPlayer(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = onlineServerPlayer(server, uuid);
        return player != null && GameFunctions.isPlayerAliveAndSurvival(player) ? player : null;
    }

    @Nullable
    private static ServerPlayerEntity onlineServerPlayer(MinecraftServer server, UUID uuid) {
        return server.getPlayerManager().getPlayer(uuid);
    }

    public interface Account {
        int balance();

        void setBalance(int balance);

        void sync();
    }
}
