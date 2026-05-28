package org.caecorthus.strawcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.world.ServerWorld;
import org.caecorthus.strawcraft.OfficialWatheWinHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MurderGameMode.class)
public abstract class MurderGameModeMixin {
    @Inject(
            method = "tickServerGameLoop(Lnet/minecraft/server/world/ServerWorld;"
                    + "Ldev/doctor4t/wathe/cca/GameWorldComponent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameRoundEndComponent;setRoundEndData"
                            + "(Ljava/util/List;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)V",
                    remap = false
            ),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void strawcraft$collectDefaultWinContributions(
            ServerWorld world,
            GameWorldComponent game,
            CallbackInfo callback,
            @Local(ordinal = 0) GameFunctions.WinStatus winStatus
    ) {
        OfficialWatheWinHooks.Decision decision =
                OfficialWatheWinHooks.evaluate(world, game, winStatus);
        if (decision.action() == OfficialWatheWinHooks.Action.SUPPRESS_DEFAULT) {
            callback.cancel();
            return;
        }
        if (decision.action() == OfficialWatheWinHooks.Action.REPLACE_DEFAULT) {
            // EN: Let official Wathe write and sync the winner screen data; StrawCraft only changes the status.
            // ZH: 让官方 Wathe 写入并同步结算界面数据；StrawCraft 只替换胜利状态。
            // CN: 仍由官方 Wathe 写入并同步结算数据；StrawCraft 只替换胜利状态。
            GameRoundEndComponent.KEY.get(world).setRoundEndData(world.getPlayers(), decision.replacementWinStatus());
            GameFunctions.stopGame(world);
            callback.cancel();
        }
    }
}
