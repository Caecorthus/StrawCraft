package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCompletionRewardPolicyTest {
    @Test
    void runtimeReadyNoellesTaskRolesReceiveTaskCompletionReward() {
        UUID player = UUID.randomUUID();

        for (Identifier roleId : List.of(
                StrawCraft.id("bartender"),
                StrawCraft.id("recaller"),
                StrawCraft.id("time_keeper"),
                StrawCraft.id("reporter"),
                StrawCraft.id("waiter")
        )) {
            assertEquals(List.of(
                    new TaskCompletionRewardPolicy.Grant(
                            player,
                            TaskCompletionRewardPolicy.TASK_COMPLETION_REWARD,
                            TaskCompletionRewardPolicy.GrantReason.NOELLES_TASK_COMPLETION
                    )
            ), TaskCompletionRewardPolicy.compute(player, roleId), roleId.toString());
        }
    }

    @Test
    void nonTaskRolesReceiveNoTaskCompletionReward() {
        UUID player = UUID.randomUUID();

        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, StrawCraft.id("civilian")));
        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, StrawCraft.id("killer")));
        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, StrawCraft.id("conductor")));
        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, StrawCraft.id("timekeeper")));
        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, null));
    }
}
