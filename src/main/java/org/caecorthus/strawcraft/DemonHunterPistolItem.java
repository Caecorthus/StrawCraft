package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class DemonHunterPistolItem extends Item {
    static final String BULLETS_KEY = "DemonHunterBullets";

    public DemonHunterPistolItem(Settings settings) {
        super(settings);
    }

    public static ItemStack createStack(int bullets) {
        ItemStack stack = new ItemStack(StrawCraftItems.DEMON_HUNTER_PISTOL);
        setBullets(stack, bullets);
        return stack;
    }

    public static boolean isDemonHunterPistol(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(StrawCraftItems.DEMON_HUNTER_PISTOL);
    }

    public static int getBullets(ItemStack stack) {
        if (!isDemonHunterPistol(stack)) {
            return 0;
        }
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!nbt.contains(BULLETS_KEY, NbtElement.INT_TYPE)) {
            return 0;
        }
        return Math.max(0, nbt.getInt(BULLETS_KEY));
    }

    public static void addBullets(ItemStack stack, int bullets) {
        setBullets(stack, getBullets(stack) + Math.max(0, bullets));
    }

    public static void setBullets(ItemStack stack, int bullets) {
        if (!isDemonHunterPistol(stack)) {
            return;
        }
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putInt(BULLETS_KEY, Math.max(0, bullets));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity shooter) || hand != Hand.MAIN_HAND) {
            return TypedActionResult.fail(stack);
        }

        ServerPlayerEntity target = findTarget(shooter);
        NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(shooter);
        DemonHunterPsychoPolicy.ShotResult result = DemonHunterPsychoPolicy.evaluateShot(
                new DemonHunterPsychoPolicy.ShotInput(
                        isDemonHunterPistol(stack),
                        getBullets(stack),
                        !shooter.getItemCooldownManager().isCoolingDown(StrawCraftItems.DEMON_HUNTER_PISTOL),
                        target != null,
                        target != null && state.hasDemonHunterFrenziedPlayer(target.getUuid()),
                        target != null && isActiveAssignedPlayer(target),
                        target != null && target.getServerWorld() == shooter.getServerWorld(),
                        target == null ? Double.POSITIVE_INFINITY : shooter.squaredDistanceTo(target),
                        target != null && shooter.canSee(target)
                )
        );
        if (!result.consumesBullet()) {
            return TypedActionResult.fail(stack);
        }

        int bulletsAfterShot = DemonHunterPsychoPolicy.bulletsAfterShot(getBullets(stack), result);
        setBullets(stack, bulletsAfterShot);
        shooter.getItemCooldownManager().set(StrawCraftItems.DEMON_HUNTER_PISTOL, DemonHunterPsychoPolicy.SHOT_COOLDOWN_TICKS);
        if (bulletsAfterShot <= 0) {
            stack.decrement(1);
        }
        if (result.killsTarget() && target != null) {
            GameFunctions.killPlayer(target, true, shooter, StrawDeathReasons.DEMON_HUNTER_SHOT);
        }
        return TypedActionResult.success(stack);
    }

    private static ServerPlayerEntity findTarget(ServerPlayerEntity shooter) {
        Vec3d start = shooter.getCameraPosVec(1.0F);
        Vec3d look = shooter.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(DemonHunterPsychoPolicy.SHOT_RANGE));
        Box searchBox = shooter.getBoundingBox()
                .stretch(look.multiply(DemonHunterPsychoPolicy.SHOT_RANGE))
                .expand(1.0D);
        EntityHitResult hit = ProjectileUtil.raycast(
                shooter,
                start,
                end,
                searchBox,
                entity -> entity instanceof ServerPlayerEntity && entity != shooter,
                DemonHunterPsychoPolicy.SHOT_RANGE_SQUARED
        );
        if (hit == null) {
            return null;
        }
        Entity entity = hit.getEntity();
        return entity instanceof ServerPlayerEntity target ? target : null;
    }

    private static boolean isActiveAssignedPlayer(ServerPlayerEntity target) {
        ServerWorld world = target.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        return game.isRunning()
                && game.getRole(target) != null
                && GameFunctions.isPlayerAliveAndSurvival(target);
    }
}
