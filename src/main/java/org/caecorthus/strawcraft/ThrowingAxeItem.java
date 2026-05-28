package org.caecorthus.strawcraft;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public final class ThrowingAxeItem extends Item {
    private static final int MAX_USE_TICKS = 72000;
    private static final float THROW_SPEED = 2.5F;
    private static final float THROW_DIVERGENCE = 1.0F;

    public ThrowingAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.isEmpty()) {
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        int chargeTicks = getMaxUseTime(stack, user) - remainingUseTicks;
        ThrowingAxePowerPolicy.Decision decision = ThrowingAxePowerPolicy.evaluateChargeTicks(chargeTicks);
        if (!decision.accepted()) {
            return;
        }
        if (world.isClient()) {
            return;
        }

        ThrowingAxeEntity axe = new ThrowingAxeEntity(world, user, stack.copyWithCount(1));
        axe.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, THROW_SPEED * decision.power(), THROW_DIVERGENCE);
        world.spawnEntity(axe);

        if (!(user instanceof PlayerEntity player) || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }
}
