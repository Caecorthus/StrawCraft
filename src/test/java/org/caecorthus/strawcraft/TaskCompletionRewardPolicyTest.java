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
                StrawCraft.id("timekeeper"),
                StrawCraft.id("reporter")
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
        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, null));
    }

    @Test
    void waiterStaysIneligibleUntilItsCatalogEntryIsRuntimeReady() {
        UUID player = UUID.randomUUID();
        Identifier waiter = StrawCraft.id("waiter");

        // Waiter is a task role design placeholder, not a runtime-ready reward recipient yet.
        // waiter 是任务职业设计占位；当前未运行就绪，因此暂不发放奖励。
        assertEquals(NoellesRoleCatalog.Readiness.DESIGN_REQUIRED, NoellesRoleCatalog.find(waiter).orElseThrow().readiness());
        assertEquals(List.of(), TaskCompletionRewardPolicy.compute(player, waiter));
    }
}
