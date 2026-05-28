package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(GameRoundEndComponent.class)
public abstract class GameRoundEndComponentMixin {
    @Shadow
    @Final
    private List<GameRoundEndComponent.RoundEndData> players;

    @Shadow
    private GameFunctions.WinStatus winStatus;

    @Inject(
            method = "didWin(Ljava/util/UUID;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void strawcraft$looseEndSoleSurvivorDidWin(UUID playerUuid, CallbackInfoReturnable<Boolean> callback) {
        if (winStatus != GameFunctions.WinStatus.LOOSE_END) {
            return;
        }

        // EN: Official Wathe stores only round-end profiles client-side; for LOOSE_END, the sole survivor is the winner.
        // ZH: 官方 Wathe 客户端只保存结算玩家资料；LOOSE_END 下唯一存活者就是胜者。
        // CN: 官方 Wathe 客户端只保存结算玩家资料；在 LOOSE_END 中，唯一存活者就是胜者。
        GameRoundEndComponent.RoundEndData soleSurvivor = null;
        for (GameRoundEndComponent.RoundEndData player : players) {
            if (player.wasDead()) {
                continue;
            }
            if (soleSurvivor != null) {
                callback.setReturnValue(false);
                return;
            }
            soleSurvivor = player;
        }
        callback.setReturnValue(soleSurvivor != null && soleSurvivor.player().getId().equals(playerUuid));
    }
}
