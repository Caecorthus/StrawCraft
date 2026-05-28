package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.caecorthus.strawcraft.TaskCompletionRewardPayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerMoodComponent.class, priority = 900)
public abstract class PlayerMoodComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking;send(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/packet/CustomPayload;)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private void strawcraft$rewardNoellesTaskCompletion(CallbackInfo callback) {
        // Official TaskCompletePayload is still sent first; this hook only adds StrawCraft economy.
        // 官方 TaskCompletePayload 仍先发送；此钩子只追加 StrawCraft 经济奖励。
        if (player instanceof ServerPlayerEntity serverPlayer) {
            TaskCompletionRewardPayout.payoutTaskCompletion(serverPlayer);
        }
    }
}
