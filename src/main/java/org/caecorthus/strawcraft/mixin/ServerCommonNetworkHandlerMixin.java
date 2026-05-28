package org.caecorthus.strawcraft.mixin;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.caecorthus.strawcraft.SpiritualistProjectionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void strawcraft$suppressSpiritualistProjectionSounds(Packet<?> packet, CallbackInfo callback) {
        if (!(packet instanceof PlaySoundS2CPacket) && !(packet instanceof PlaySoundFromEntityS2CPacket)) {
            return;
        }
        if ((Object) this instanceof ServerPlayNetworkHandler handler
                && SpiritualistProjectionRuntime.shouldSuppressSoundPacket(handler.player, packet)) {
            callback.cancel();
        }
    }
}
