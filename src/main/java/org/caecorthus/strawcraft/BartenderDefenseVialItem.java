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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public final class BartenderDefenseVialItem extends Item {
    public static final int USE_TIME_TICKS = 24;
    public static final int POST_SUCCESS_COOLDOWN_TICKS = 20 * 20;

    public BartenderDefenseVialItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.consume(stack);
        }
        if (!(user instanceof ServerPlayerEntity player) || findDefenseTarget(player) == BartenderDefenseVialPolicy.DefenseTarget.NONE) {
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player && clearSelfPoison(player)) {
            player.getItemCooldownManager().set(StrawCraftItems.DEFENSE_VIAL, POST_SUCCESS_COOLDOWN_TICKS);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TIME_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    private static boolean clearSelfPoison(ServerPlayerEntity player) {
        if (findDefenseTarget(player) != BartenderDefenseVialPolicy.DefenseTarget.SELF) {
            return false;
        }
        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(player);
        // Bartender defense is self-only: clear Wathe poison state, never nearby players.
        // 酒保防御仅作用于自己：只清除 Wathe 中自己的中毒状态，不影响附近玩家。
        // 酒保防御仅作用于自身：清除 Wathe 中毒状态，不治疗附近玩家。
        poisonComponent.reset();
        return true;
    }

    private static BartenderDefenseVialPolicy.DefenseTarget findDefenseTarget(ServerPlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(player);
        return BartenderDefenseVialPolicy.selectDefenseTarget(new BartenderDefenseVialPolicy.Input(
                role,
                GameFunctions.isPlayerAliveAndSurvival(player),
                poisonComponent.poisonTicks > 0
        ));
    }
}
