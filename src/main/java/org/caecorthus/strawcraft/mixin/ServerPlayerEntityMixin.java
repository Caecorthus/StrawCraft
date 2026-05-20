package org.caecorthus.strawcraft.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.caecorthus.strawcraft.WatheDeathReasonTracker;
import org.caecorthus.strawcraft.WatheRoundParticipantLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = ServerPlayerEntity.class, priority = 900)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void strawcraft$markWathePlayerDeadAfterVanillaDeath(DamageSource damageSource, CallbackInfo callback) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        WatheRoundParticipantLifecycle.afterVanillaDeath(player);
    }

    @ModifyArgs(
            method = "wathe$interceptVanillaDeath",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V"),
            remap = false,
            require = 0
    )
    private void strawcraft$useTrackedWatheDeathAttribution(Args args) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        WatheDeathReasonTracker.DeathAttribution attribution = WatheDeathReasonTracker
                .consumeDeathAttribution(player.getUuid(), args.get(3))
                .orElseThrow();
        args.set(2, strawcraft$resolveTrackedKiller(player, attribution));
        args.set(3, attribution.deathReason());
    }

    private ServerPlayerEntity strawcraft$resolveTrackedKiller(ServerPlayerEntity victim, WatheDeathReasonTracker.DeathAttribution attribution) {
        ServerPlayerEntity killer = attribution.killerUuid()
                .map(killerUuid -> victim.getServer().getPlayerManager().getPlayer(killerUuid))
                .orElse(null);
        if (killer == null || killer.getWorld() != victim.getWorld()) {
            return null;
        }
        return killer;
    }
}
