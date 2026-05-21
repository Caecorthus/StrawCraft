package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.caecorthus.strawcraft.WatheRoundParticipantLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerEntity.class, priority = 900)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void strawcraft$markWathePlayerDeadAfterVanillaDeath(DamageSource damageSource, CallbackInfo callback) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        // Run after vanilla death has finished so Wathe observes the final dead/alive state.
        // 等原版死亡流程结束后再运行，确保 Wathe 看到最终的生死状态。
        WatheRoundParticipantLifecycle.afterVanillaDeath(player);
    }
}
