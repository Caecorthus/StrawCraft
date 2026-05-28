package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public final class ThrowingAxeEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private static final double MAX_HIT_RANGE_SQUARED = 48.0D * 48.0D;

    private final ThrowingAxeHitPolicy hitPolicy = new ThrowingAxeHitPolicy();

    public ThrowingAxeEntity(EntityType<? extends ThrowingAxeEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    public ThrowingAxeEntity(World world, LivingEntity owner, ItemStack stack) {
        super(StrawCraftEntities.THROWING_AXE, owner, world, stack, null);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity hitEntity = entityHitResult.getEntity();
        Entity ownerEntity = getOwner();
        if (!(hitEntity instanceof ServerPlayerEntity target) || !(ownerEntity instanceof ServerPlayerEntity owner)) {
            return;
        }
        if (target.getServerWorld() != owner.getServerWorld()
                || owner.squaredDistanceTo(target) > MAX_HIT_RANGE_SQUARED
                || !owner.canSee(target)) {
            return;
        }

        ThrowingAxeHitPolicy.Result result = hitPolicy.evaluateHit(new ThrowingAxeHitPolicy.Input(
                owner.getUuid(),
                target.getUuid(),
                target.isAlive(),
                GameFunctions.isPlayerAliveAndSurvival(target)
        ));
        if (!result.killsTarget()) {
            return;
        }

        GameFunctions.killPlayer(target, true, owner, StrawDeathReasons.THROWING_AXE);
        discard();
    }

    @Override
    protected boolean canHit(Entity entity) {
        Entity owner = getOwner();
        if (!(entity instanceof ServerPlayerEntity target) || !(owner instanceof ServerPlayerEntity serverOwner)) {
            return false;
        }
        return super.canHit(entity)
                && target != serverOwner
                && target.getServerWorld() == serverOwner.getServerWorld()
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && serverOwner.squaredDistanceTo(target) <= MAX_HIT_RANGE_SQUARED
                && serverOwner.canSee(target)
                && !hitPolicy.hasHit(target.getUuid());
    }

    @Override
    protected boolean tryPickup(net.minecraft.entity.player.PlayerEntity player) {
        return false;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(StrawCraftItems.THROWING_AXE);
    }

    @Override
    public ItemStack getStack() {
        return getItemStack();
    }
}
