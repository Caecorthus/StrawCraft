package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class TaskCompletionRewardPayout {
    private TaskCompletionRewardPayout() {
    }

    public static void payoutTaskCompletion(ServerPlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        List<TaskCompletionRewardPolicy.Grant> grants = TaskCompletionRewardPolicy.compute(
                player.getUuid(),
                StrawRoleMeaning.roleIdFor(role).orElse(null)
        );
        apply(grants, recipientUuid -> accountFor(player, recipientUuid));
    }

    public static void apply(
            List<TaskCompletionRewardPolicy.Grant> grants,
            Function<UUID, @Nullable Account> accounts
    ) {
        Map<UUID, Integer> totals = new HashMap<>();
        for (TaskCompletionRewardPolicy.Grant grant : grants) {
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
            // Use Wathe's shop mutation path, then force a sync for this add-on reward.
            // 走 Wathe 商店余额变更入口，并为本模组奖励强制同步一次。
            account.addToBalance(amount);
            account.sync();
        });
    }

    @Nullable
    private static Account accountFor(ServerPlayerEntity player, UUID uuid) {
        if (!player.getUuid().equals(uuid)) {
            return null;
        }
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        return new Account() {
            @Override
            public void addToBalance(int amount) {
                shop.addToBalance(amount);
            }

            @Override
            public void sync() {
                shop.sync();
            }
        };
    }

    public interface Account {
        void addToBalance(int amount);

        void sync();
    }
}
