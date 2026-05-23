package org.caecorthus.strawcraft.mixin;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.caecorthus.strawcraft.StrawShopPurchaseFlow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerShopComponent.class, priority = 900)
public abstract class PlayerShopComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    public int balance;

    @Shadow
    public abstract void sync();

    @Inject(method = "tryBuy", at = @At("HEAD"), cancellable = true)
    private void strawcraft$buyThroughAddonPurchaseEvents(int index, CallbackInfo callback) {
        // StrawCraft owns the add-on purchase transaction, while StoreBuyPayload(index) stays Wathe-native.
        // StrawCraft 只接管附加事件和交易顺序，StoreBuyPayload(index) 仍保持 Wathe 原生契约。
        StrawShopPurchaseFlow.tryBuy(player, index, new StrawShopPurchaseFlow.Account() {
            @Override
            public int balance() {
                return balance;
            }

            @Override
            public void setBalance(int nextBalance) {
                balance = nextBalance;
            }

            @Override
            public void sync() {
                PlayerShopComponentMixin.this.sync();
            }
        });
        callback.cancel();
    }
}
