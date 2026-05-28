package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class PoisonNeedleItem extends Item {
    public PoisonNeedleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity poisoner) || !(entity instanceof ServerPlayerEntity target)) {
            return ActionResult.FAIL;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role role = game.getRole(poisoner);
        PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(target);
        PoisonNeedlePolicy.Decision decision = PoisonNeedlePolicy.evaluateUse(new PoisonNeedlePolicy.Input(
                role,
                game.isRunning(),
                GameFunctions.isPlayerAliveAndSurvival(poisoner),
                GameFunctions.isPlayerAliveAndSurvival(target),
                !target.isSpectator() && !target.isCreative(),
                poisoner.getUuid().equals(target.getUuid()),
                poison.poisonTicks,
                !poisoner.getItemCooldownManager().isCoolingDown(StrawCraftItems.POISON_NEEDLE)
        ));
        if (!decision.allowed()) {
            return ActionResult.FAIL;
        }

        poison.poisonTicks = decision.poisonTicksAfterUse();
        PlayerPoisonComponent.KEY.sync(target);
        poisoner.getItemCooldownManager().set(StrawCraftItems.POISON_NEEDLE, PoisonNeedlePolicy.COOLDOWN_TICKS);
        if (!poisoner.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
