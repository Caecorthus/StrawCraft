package org.caecorthus.strawcraft;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public final class WaiterServiceRuntime {
    private WaiterServiceRuntime() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(WaiterServiceRuntime::useEntity);
    }

    private static ActionResult useEntity(
            PlayerEntity player,
            World world,
            Hand hand,
            Entity entity,
            EntityHitResult hitResult
    ) {
        if (player == null || world == null || hand == null || entity == null || world.isClient()) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        WaiterFeedingPolicy.ServiceKind serviceKind = WaiterServiceItem.serviceKindFor(stack);
        if (serviceKind == WaiterFeedingPolicy.ServiceKind.INVALID) {
            return ActionResult.PASS;
        }

        // English: Let held food/cocktails reach the same server-side policy as the tray item.
        // 中文：让手持食物/鸡尾酒走与托盘道具相同的服务端校验策略。
        ActionResult result = WaiterServiceItem.tryServe(stack, player, entity, hand);
        if (result == ActionResult.FAIL && serviceKind != WaiterFeedingPolicy.ServiceKind.SERVICE) {
            return ActionResult.PASS;
        }
        return result;
    }
}
