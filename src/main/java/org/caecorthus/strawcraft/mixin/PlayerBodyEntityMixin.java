package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import org.caecorthus.strawcraft.ScavengerHiddenBodyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerBodyEntity.class, priority = 900)
public abstract class PlayerBodyEntityMixin implements ScavengerHiddenBodyEntity {
    @Unique
    private static final String STRAWCRAFT_HIDDEN_BY_SCAVENGER_NBT_KEY = "StrawCraftHiddenByScavenger";

    // A tracked flag lets official Wathe bodies carry StrawCraft visibility without changing Wathe code.
    // tracked flag 让官方 Wathe 尸体携带 StrawCraft 可见性状态，不需要改 Wathe 源码。
    @Unique
    private static final TrackedData<Boolean> STRAWCRAFT_HIDDEN_BY_SCAVENGER =
            DataTracker.registerData(PlayerBodyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void strawcraft$trackScavengerHiddenBody(DataTracker.Builder builder, CallbackInfo callbackInfo) {
        builder.add(STRAWCRAFT_HIDDEN_BY_SCAVENGER, false);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void strawcraft$writeScavengerHiddenBody(NbtCompound nbt, CallbackInfo callbackInfo) {
        nbt.putBoolean(STRAWCRAFT_HIDDEN_BY_SCAVENGER_NBT_KEY, strawcraft$isHiddenByScavenger());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void strawcraft$readScavengerHiddenBody(NbtCompound nbt, CallbackInfo callbackInfo) {
        strawcraft$setHiddenByScavenger(nbt.getBoolean(STRAWCRAFT_HIDDEN_BY_SCAVENGER_NBT_KEY));
    }

    @Override
    public void strawcraft$setHiddenByScavenger(boolean hidden) {
        ((PlayerBodyEntity) (Object) this).getDataTracker().set(STRAWCRAFT_HIDDEN_BY_SCAVENGER, hidden);
    }

    @Override
    public boolean strawcraft$isHiddenByScavenger() {
        return ((PlayerBodyEntity) (Object) this).getDataTracker().get(STRAWCRAFT_HIDDEN_BY_SCAVENGER);
    }
}
