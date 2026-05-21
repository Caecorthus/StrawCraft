package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import org.caecorthus.strawcraft.GrenadeExplosionDamage;
import org.caecorthus.strawcraft.WatheDeathReasonTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = GrenadeEntity.class, priority = 900)
public abstract class GrenadeEntityMixin {
    @Redirect(
            method = "onCollision",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getPlayers(Ljava/util/function/Predicate;)Ljava/util/List;")
    )
    private List<ServerPlayerEntity> strawcraft$getPlayersInSphericalBlast(ServerWorld world, Predicate<? super ServerPlayerEntity> originalPredicate) {
        return world.getPlayers(this::strawcraft$isPlayerInsideSphericalBlast);
    }

    @Redirect(
            method = "onCollision",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V")
    )
    private void strawcraft$applyVanillaBlastDamage(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier deathReason) {
        if (!(victim instanceof ServerPlayerEntity serverVictim)) {
            return;
        }

        Entity grenade = (Entity) (Object) this;
        Vec3d center = grenade.getPos();
        float exposure = Explosion.getExposure(center, serverVictim);
        double distance = serverVictim.getBoundingBox().getCenter().distanceTo(center);
        float damage = GrenadeExplosionDamage.damageAt(distance, exposure);
        if (damage <= 0.0f) {
            return;
        }

        DamageSource source = serverVictim.getDamageSources().explosion(grenade, killer);
        WatheDeathReasonTracker.damageWithReason(serverVictim, deathReason, source, damage);
    }

    private boolean strawcraft$isPlayerInsideSphericalBlast(ServerPlayerEntity player) {
        Entity grenade = (Entity) (Object) this;
        double closestBodyDistanceSquared = player.getBoundingBox().squaredMagnitude(grenade.getPos());
        return closestBodyDistanceSquared <= GrenadeExplosionDamage.RADIUS * GrenadeExplosionDamage.RADIUS
                && GameFunctions.isPlayerAliveAndSurvival(player);
    }
}
