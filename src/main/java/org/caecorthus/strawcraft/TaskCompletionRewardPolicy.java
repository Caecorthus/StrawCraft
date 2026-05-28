package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TaskCompletionRewardPolicy {
    public static final int TASK_COMPLETION_REWARD = 50;

    private static final Set<Identifier> TASK_REWARD_ROLE_IDS = Set.of(
            StrawCraft.id("bartender"),
            StrawCraft.id("recaller"),
            StrawCraft.id("timekeeper"),
            StrawCraft.id("reporter"),
            StrawCraft.id("waiter")
    );

    private TaskCompletionRewardPolicy() {
    }

    public static List<Grant> compute(UUID recipientUuid, @Nullable Identifier roleId) {
        if (!isEligible(roleId)) {
            return List.of();
        }
        return List.of(new Grant(recipientUuid, TASK_COMPLETION_REWARD, GrantReason.NOELLES_TASK_COMPLETION));
    }

    public static boolean isEligible(@Nullable Identifier roleId) {
        return roleId != null
                && TASK_REWARD_ROLE_IDS.contains(roleId)
                && NoellesRoleCatalog.find(roleId)
                .filter(entry -> entry.faction() == StrawFaction.GOOD)
                .filter(NoellesRoleCatalog.Entry::isRuntimeReady)
                .isPresent();
    }

    public record Grant(UUID recipientUuid, int amount, GrantReason reason) {
    }

    public enum GrantReason {
        NOELLES_TASK_COMPLETION
    }
}
