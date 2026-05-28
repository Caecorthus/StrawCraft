package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMoodTaskCompletionMixinContractTest {
    private static final String PLAYER_MOOD_COMPONENT = "dev.doctor4t.wathe.cca.PlayerMoodComponent";
    private static final String PLAYER_MOOD_MIXIN = "org.caecorthus.strawcraft.mixin.PlayerMoodComponentMixin";
    private static final String SERVER_PLAY_NETWORKING_SEND =
            "Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking;send("
                    + "Lnet/minecraft/server/network/ServerPlayerEntity;"
                    + "Lnet/minecraft/network/packet/CustomPayload;)V";

    @Test
    void officialWatheServerTickStillSendsExactlyOneTaskCompletePayload() {
        assertTrue(MixinTargetBytecode.hasMethod(
                PLAYER_MOOD_COMPONENT,
                "serverTick",
                "()V"
        ));
        assertEquals(1, MixinTargetBytecode.countTypeCreations(
                PLAYER_MOOD_COMPONENT,
                "serverTick",
                "()V",
                "dev/doctor4t/wathe/util/TaskCompletePayload"
        ), "Wathe PlayerMoodComponent.serverTick should still create one TaskCompletePayload.");
        assertEquals(1, MixinTargetBytecode.countMethodInvocations(
                PLAYER_MOOD_COMPONENT,
                "serverTick",
                "()V",
                "net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking",
                "send",
                "(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/packet/CustomPayload;)V"
        ), "Wathe PlayerMoodComponent.serverTick should still send one task-complete payload.");
    }

    @Test
    void strawCraftRewardMixinStaysOnTheOfficialTaskCompleteSendSeam() {
        MixinTargetBytecode.MixinClassInspection mixin = MixinTargetBytecode.inspectMixinClass(
                PLAYER_MOOD_MIXIN,
                PLAYER_MOOD_COMPONENT
        );

        assertTrue(mixin.classPresent());
        assertTrue(mixin.targetsExpectedOwner());
        assertTrue(mixin.injections().stream().anyMatch(injection ->
                "@Inject".equals(injection.annotationName())
                        && injection.targetsMethod("serverTick")
                        && injection.targetsAt(SERVER_PLAY_NETWORKING_SEND)
        ), mixin.describeInjections());
        assertTrue(mixin.hasSoftInjectionFor("serverTick"), mixin.describeInjections());
    }
}
