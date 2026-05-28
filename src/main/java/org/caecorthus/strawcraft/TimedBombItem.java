package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class TimedBombItem extends Item {
    public TimedBombItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity bomber) || !(entity instanceof ServerPlayerEntity target)) {
            return ActionResult.FAIL;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role role = game.getRole(bomber);
        NoellesRoleStateComponent targetState = NoellesRoleStateComponent.KEY.get(target);
        BomberTimedBombPolicy.AttachResult result = BomberTimedBombPolicy.validateAttach(new BomberTimedBombPolicy.AttachInput(
                game.isRunning(),
                StrawRoleMeaning.usesBomberShop(role),
                GameFunctions.isPlayerAliveAndSurvival(bomber),
                GameFunctions.isPlayerAliveAndSurvival(target),
                bomber.getUuid().equals(target.getUuid()),
                targetState.timedBomb().isPresent(),
                !bomber.getItemCooldownManager().isCoolingDown(StrawCraftItems.TIMED_BOMB)
        ));
        if (result != BomberTimedBombPolicy.AttachResult.ALLOWED) {
            return ActionResult.FAIL;
        }

        targetState.setTimedBomb(BomberTimedBombPolicy.createBomb(bomber.getUuid(), target.getUuid(), world.getTime()));
        bomber.getItemCooldownManager().set(StrawCraftItems.TIMED_BOMB, BomberTimedBombPolicy.ATTACH_COOLDOWN_TICKS);
        if (!bomber.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
