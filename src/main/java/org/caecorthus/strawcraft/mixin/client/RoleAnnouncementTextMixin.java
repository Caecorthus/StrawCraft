package org.caecorthus.strawcraft.mixin.client;

import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.client.NoellesNeutralWinClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RoleAnnouncementTexts.RoleAnnouncementText.class)
public abstract class RoleAnnouncementTextMixin {
    @Inject(method = "getEndText", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void strawcraft$neutralRoleWinText(
            GameFunctions.WinStatus winStatus,
            Text looseEndWinnerName,
            CallbackInfoReturnable<Text> callback
    ) {
        NoellesNeutralWinClientState.endTextFor(winStatus, looseEndWinnerName)
                .ifPresent(callback::setReturnValue);
    }
}
