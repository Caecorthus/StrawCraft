package org.caecorthus.strawcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.caecorthus.strawcraft.client.MorphlingClientVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RoleNameRenderer.class)
public abstract class MorphlingRoleNameRendererMixin {
    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private static void strawcraft$hideMorphlingCorpseRoleHud(
            TextRenderer textRenderer,
            ClientPlayerEntity player,
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo callbackInfo
    ) {
        if (!(MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult hitResult)
                || !(hitResult.getEntity() instanceof PlayerEntity target)) {
            return;
        }
        if (MorphlingClientVisuals.isCorpseMode(target)) {
            callbackInfo.cancel();
        }
    }

    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"
            )
    )
    private static Text strawcraft$useMorphlingDisguiseRoleHudName(
            PlayerEntity target,
            Operation<Text> original
    ) {
        if (MorphlingClientVisuals.isCorpseMode(target)) {
            return Text.empty();
        }
        return MorphlingClientVisuals.disguiseDisplayName(target).orElseGet(() -> original.call(target));
    }
}
