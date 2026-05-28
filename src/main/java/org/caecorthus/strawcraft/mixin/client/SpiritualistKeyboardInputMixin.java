package org.caecorthus.strawcraft.mixin.client;

import net.minecraft.client.input.KeyboardInput;
import org.caecorthus.strawcraft.client.SpiritualistProjectionClientView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class SpiritualistKeyboardInputMixin extends net.minecraft.client.input.Input {
    @Inject(method = "tick(ZF)V", at = @At("TAIL"))
    private void strawcraft$suppressRealMovementDuringProjection(
            boolean slowDown,
            float slowDownFactor,
            CallbackInfo callbackInfo
    ) {
        if (!SpiritualistProjectionClientView.isProjecting()) {
            return;
        }

        // Projection movement is consumed by the client-only marker; keep the real body still.
        // 投射移动只交给客户端 Marker 消耗；真实身体保持静止。
        pressingForward = false;
        pressingBack = false;
        pressingLeft = false;
        pressingRight = false;
        movementForward = 0.0F;
        movementSideways = 0.0F;
        jumping = false;
        sneaking = false;
    }
}
