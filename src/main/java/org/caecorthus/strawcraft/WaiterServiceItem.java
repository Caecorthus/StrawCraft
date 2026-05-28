package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Optional;

public final class WaiterServiceItem extends Item {
    public WaiterServiceItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        return tryServe(stack, user, entity, hand);
    }

    static ActionResult tryServe(ItemStack stack, PlayerEntity user, Entity entity, Hand hand) {
        if (user == null || user.getWorld().isClient()) {
            return ActionResult.CONSUME;
        }
        if (!(user instanceof ServerPlayerEntity waiter) || !(entity instanceof ServerPlayerEntity target)) {
            return ActionResult.FAIL;
        }

        PlayerMoodComponent targetMood = PlayerMoodComponent.KEY.get(target);
        Role role = GameWorldComponent.KEY.get(waiter.getWorld()).getRole(waiter);
        Optional<WaiterFeedingPolicy.Service> service = WaiterFeedingPolicy.chooseService(new WaiterFeedingPolicy.Input(
                role,
                GameFunctions.isPlayerAliveAndSurvival(waiter),
                GameFunctions.isPlayerAliveAndSurvival(target),
                waiter.getUuid().equals(target.getUuid()),
                serviceKindFor(stack),
                hasOpenTask(targetMood, target, PlayerMoodComponent.Task.EAT),
                hasOpenTask(targetMood, target, PlayerMoodComponent.Task.DRINK)
        ));
        if (service.isEmpty()) {
            return ActionResult.FAIL;
        }

        if (isWaiterServiceTray(stack)) {
            satisfyTask(targetMood, service.get());
            if (!waiter.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        } else {
            // Official food/cocktail consumption preserves Wathe's task and poison item seams.
            // 官方食物/鸡尾酒消费会保留 Wathe 的任务与带毒物品衔接点。
            waiter.setStackInHand(hand, stack.finishUsing(target.getWorld(), target));
        }
        return ActionResult.SUCCESS;
    }

    static WaiterFeedingPolicy.ServiceKind serviceKindFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return WaiterFeedingPolicy.ServiceKind.INVALID;
        }
        if (isWaiterServiceTray(stack)) {
            return WaiterFeedingPolicy.ServiceKind.SERVICE;
        }
        if (stack.getItem() instanceof CocktailItem) {
            return WaiterFeedingPolicy.ServiceKind.DRINK;
        }
        return stack.get(DataComponentTypes.FOOD) == null
                ? WaiterFeedingPolicy.ServiceKind.INVALID
                : WaiterFeedingPolicy.ServiceKind.FOOD;
    }

    private static boolean isWaiterServiceTray(ItemStack stack) {
        return StrawCraftItems.WAITER_SERVICE_TRAY != null && stack.isOf(StrawCraftItems.WAITER_SERVICE_TRAY);
    }

    private static boolean hasOpenTask(PlayerMoodComponent mood, PlayerEntity target, PlayerMoodComponent.Task task) {
        PlayerMoodComponent.TrainTask trainTask = mood.tasks.get(task);
        return trainTask != null && !trainTask.isFulfilled(target);
    }

    private static void satisfyTask(PlayerMoodComponent mood, WaiterFeedingPolicy.Service service) {
        if (service == WaiterFeedingPolicy.Service.EAT) {
            mood.eatFood();
        } else {
            mood.drinkCocktail();
        }
    }
}
