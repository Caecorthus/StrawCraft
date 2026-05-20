package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.WatheDeathReasonTracker;
import org.caecorthus.strawcraft.WatheRoundParticipantLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerEntity.class, priority = 900)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void strawcraft$markWathePlayerDeadAfterVanillaDeath(DamageSource damageSource, CallbackInfo callback) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        WatheRoundParticipantLifecycle.afterVanillaDeath(player);
    }

    @ModifyArg(
            method = "wathe$interceptVanillaDeath",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V"),
            index = 3,
            remap = false,
            require = 0
    )
    private Identifier strawcraft$useTrackedWatheDeathReason(Identifier vanillaDeathReason) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        return WatheDeathReasonTracker.consumeDeathReason(player.getUuid(), vanillaDeathReason);
    }
}
