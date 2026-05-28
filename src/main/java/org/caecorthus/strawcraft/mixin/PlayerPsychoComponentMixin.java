package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.caecorthus.strawcraft.DemonHunterPsychoRuntime;
import org.caecorthus.strawcraft.SilentPsychoModeAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerPsychoComponent.class, priority = 900)
public abstract class PlayerPsychoComponentMixin implements SilentPsychoModeAccess {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    public int psychoTicks;

    @Unique
    private boolean strawcraft$silentPsycho;

    @Shadow
    public abstract void setPsychoTicks(int ticks);

    @Shadow
    public abstract void setArmour(int armour);

    @Override
    public boolean strawcraft$startSilentPsycho() {
        if (!ShopEntry.insertStackInFreeSlot(player, new ItemStack(WatheItems.BAT))) {
            return false;
        }

        // EN: Mirror Wathe psycho startup while leaving the global psycho counter untouched.
        // 中文：复刻 Wathe 的 psycho 启动效果，但不触碰全局 psycho 计数器。
        // ZH: 复刻 Wathe 疯魔启动逻辑，但不改动全局疯魔计数。
        strawcraft$silentPsycho = true;
        setPsychoTicks(GameConstants.PSYCHO_TIMER);
        setArmour(1);
        return true;
    }

    @Inject(method = "startPsycho()Z", at = @At("RETURN"), require = 0)
    private void strawcraft$rememberLoudPsychoMode(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            strawcraft$silentPsycho = false;
            DemonHunterPsychoRuntime.onLoudPsychoStarted(player);
        }
    }

    @Inject(method = "stopPsycho()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void strawcraft$stopSilentPsychoWithoutGlobalCounter(CallbackInfo callback) {
        if (!strawcraft$silentPsycho) {
            DemonHunterPsychoRuntime.onLoudPsychoStopped(player);
            return;
        }

        // EN: Official stopPsycho decrements psychosActive; silent starts never incremented it.
        // 中文：官方 stopPsycho 会递减 psychosActive；静默启动从未递增它。
        // ZH: 官方 stopPsycho 会递减 psychosActive；静默启动从未递增该计数。
        strawcraft$silentPsycho = false;
        psychoTicks = 0;
        player.getInventory().remove(
                itemStack -> itemStack.isOf(WatheItems.BAT),
                Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput()
        );
        callback.cancel();
    }

    @Inject(method = "writeToNbt", at = @At("TAIL"), require = 0)
    private void strawcraft$writeSilentPsycho(
            NbtCompound tag,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo callback
    ) {
        tag.putBoolean("strawcraftSilentPsycho", strawcraft$silentPsycho);
    }

    @Inject(method = "readFromNbt", at = @At("TAIL"), require = 0)
    private void strawcraft$readSilentPsycho(
            NbtCompound tag,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo callback
    ) {
        strawcraft$silentPsycho = tag.getBoolean("strawcraftSilentPsycho");
    }
}
