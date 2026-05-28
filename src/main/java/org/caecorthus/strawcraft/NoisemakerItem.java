package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class NoisemakerItem extends Item {
    public NoisemakerItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user == null ? ItemStack.EMPTY : user.getStackInHand(hand);
        if (world == null || user == null || world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.fail(stack);
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role role = game.getRole(player);
        boolean cooldownReady = !player.getItemCooldownManager().isCoolingDown(StrawCraftItems.NOISEMAKER);

        // Gate the audible effect through Wathe's round/role state.
        // 通过 Wathe 的回合和职业状态限制可听效果。
        // 通过 Wathe 的回合与职业状态来决定是否触发可听见的效果。
        if (!NoisemakerEffectPolicy.shouldTrigger(new NoisemakerEffectPolicy.Input(
                world.isClient(),
                game.isRunning(),
                role,
                GameFunctions.isPlayerAliveAndSurvival(player),
                cooldownReady
        ))) {
            return TypedActionResult.fail(stack);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_GOAT_HORN_PLAY, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.getItemCooldownManager().set(StrawCraftItems.NOISEMAKER, NoisemakerEffectPolicy.COOLDOWN_TICKS);
        return TypedActionResult.success(stack);
    }
}
