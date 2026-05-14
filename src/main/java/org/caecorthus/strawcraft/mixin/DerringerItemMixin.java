package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.item.DerringerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DerringerItem.class, priority = 900)
public abstract class DerringerItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void strawcraft$disableDerringer(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> callback) {
        callback.setReturnValue(TypedActionResult.fail(user.getStackInHand(hand)));
    }
}
