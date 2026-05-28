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
import org.jetbrains.annotations.Nullable;

public final class ToxicologistAntidoteItem extends Item {
    public static final int USE_TIME_TICKS = 32;
    public static final int INITIAL_ASSIGNMENT_COOLDOWN_TICKS = 10 * 20;
    public static final int POST_SUCCESS_COOLDOWN_TICKS = 30 * 20;

    public ToxicologistAntidoteItem(Settings settings) {
        super(settings);
    }

    public static void applyInitialAssignmentCooldown(PlayerEntity player) {
        // Use Minecraft's item cooldown so the assignment delay is real client-visible state.
        // 使用 Minecraft 物品冷却，让初始延迟成为客户端可见的真实状态。
        if (player != null && StrawCraftItems.ANTIDOTE != null) {
            player.getItemCooldownManager().set(StrawCraftItems.ANTIDOTE, INITIAL_ASSIGNMENT_COOLDOWN_TICKS);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.consume(stack);
        }
        if (!(user instanceof ServerPlayerEntity player) || findCureTarget(player) == null) {
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player && curePoison(player)) {
            player.getItemCooldownManager().set(StrawCraftItems.ANTIDOTE, POST_SUCCESS_COOLDOWN_TICKS);
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

    private static boolean curePoison(ServerPlayerEntity carrier) {
        PlayerEntity target = findCureTarget(carrier);
        if (target == null) {
            return false;
        }

        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(target);
        // Official Wathe owns poison state; StrawCraft only asks that component to clear itself.
        // 毒状态仍由官方 Wathe 持有；StrawCraft 这里只请求官方组件自行清除。
        poisonComponent.reset();
        return true;
    }

    @Nullable
    private static PlayerEntity findCureTarget(ServerPlayerEntity carrier) {
        Role role = GameWorldComponent.KEY.get(carrier.getWorld()).getRole(carrier);
        PlayerPoisonComponent selfPoison = PlayerPoisonComponent.KEY.get(carrier);
        PlayerEntity nearbyTarget = findNearbyPoisonedPlayer(carrier);
        ToxicologistAntidotePolicy.CureTarget target = ToxicologistAntidotePolicy.selectCureTarget(
                new ToxicologistAntidotePolicy.Input(
                        role,
                        GameFunctions.isPlayerAliveAndSurvival(carrier),
                        isPoisoned(selfPoison),
                        nearbyTarget != null,
                        nearbyTarget != null && GameFunctions.isPlayerAliveAndSurvival(nearbyTarget),
                        nearbyTarget != null && isPoisoned(PlayerPoisonComponent.KEY.get(nearbyTarget)),
                        nearbyTarget == null ? Double.POSITIVE_INFINITY : carrier.squaredDistanceTo(nearbyTarget)
                )
        );
        return switch (target) {
            case SELF -> carrier;
            case NEARBY_PLAYER -> nearbyTarget;
            case NONE -> null;
        };
    }

    @Nullable
    private static PlayerEntity findNearbyPoisonedPlayer(ServerPlayerEntity carrier) {
        PlayerEntity nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (ServerPlayerEntity candidate : carrier.getServerWorld().getPlayers()) {
            if (candidate == carrier) {
                continue;
            }
            double squaredDistance = carrier.squaredDistanceTo(candidate);
            if (squaredDistance > ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED || squaredDistance >= nearestDistance) {
                continue;
            }
            if (GameFunctions.isPlayerAliveAndSurvival(candidate) && isPoisoned(PlayerPoisonComponent.KEY.get(candidate))) {
                nearest = candidate;
                nearestDistance = squaredDistance;
            }
        }
        return nearest;
    }

    private static boolean isPoisoned(PlayerPoisonComponent poisonComponent) {
        return poisonComponent.poisonTicks > 0;
    }
}
