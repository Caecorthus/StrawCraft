package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.caecorthus.strawcraft.TaczAmmoRefillTimers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerEntity.class, priority = 900)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "wathe$interceptVanillaDeath", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void strawcraft$allowVanillaDeath(DamageSource damageSource, CallbackInfo originalDeathCallback, CallbackInfo handlerCallback) {
        // Do not let Wathe convert vanilla death into its spectator/body death pipeline.
        handlerCallback.cancel();
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void strawcraft$markWathePlayerDeadAfterVanillaDeath(DamageSource damageSource, CallbackInfo callback) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        TaczAmmoRefillTimers.clearPlayer(player);
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (game.isRunning() && game.hasAnyRole(player.getUuid()) && !game.isPlayerDead(player.getUuid())) {
            // Keep Wathe's win-condition bookkeeping aware of vanilla deaths.
            game.markPlayerDead(player.getUuid());
            game.sync();
        }
    }
}
