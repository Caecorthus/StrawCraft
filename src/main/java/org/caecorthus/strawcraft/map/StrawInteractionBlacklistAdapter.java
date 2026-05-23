package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public final class StrawInteractionBlacklistAdapter {
    private StrawInteractionBlacklistAdapter() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(StrawInteractionBlacklistAdapter::onUseBlock);
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game == null || !game.isRunning() || !game.getRoles().containsKey(player.getUuid())) {
            return ActionResult.PASS;
        }

        Optional<StrawMapEnhancements.InteractionBlacklist> blacklist = activeBlacklist(world, game);
        if (blacklist.isEmpty() || blacklist.get().isEmpty()) {
            return ActionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        return blacklist.get().blocksInteraction(world.getBlockState(pos))
                ? ActionResult.FAIL
                : ActionResult.PASS;
    }

    private static Optional<StrawMapEnhancements.InteractionBlacklist> activeBlacklist(World world, GameWorldComponent game) {
        return StrawCurrentMapResolver.resolve(world, game)
                .map(StrawMapEntry::enhancements)
                .map(StrawMapEnhancements::interactionBlacklist);
    }
}
