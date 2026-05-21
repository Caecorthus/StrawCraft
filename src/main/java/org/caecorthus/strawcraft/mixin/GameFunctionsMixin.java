package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.MapEffect;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.map.StrawMapVoting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class GameFunctionsMixin {
    @Inject(method = "startGame", at = @At("HEAD"), cancellable = true)
    private static void strawcraft$blockStartWhileMapVoteIsActive(
            ServerWorld world,
            GameMode gameMode,
            MapEffect mapEffect,
            int time,
            CallbackInfo ci
    ) {
        if (!StrawMapVoting.isVotingActive(world.getServer())) {
            return;
        }

        // Map voting sits between Wathe rounds; block accidental starts until
        // the selected destination has been applied to the Wathe game component.
        // 地图投票位于两局 Wathe 之间；在选中的目标写入 Wathe 游戏组件前，
        // 阻止误触发的新一局开始。
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.translatable("game.strawcraft.start_error.voting_active"), true);
        }
        ci.cancel();
    }
}
