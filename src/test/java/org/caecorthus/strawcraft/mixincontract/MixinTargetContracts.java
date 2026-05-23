package org.caecorthus.strawcraft.mixincontract;

import java.util.List;

import static org.caecorthus.strawcraft.mixincontract.MixinTargetContract.Requirement.REQUIRED;

final class MixinTargetContracts {
    private MixinTargetContracts() {
    }

    static List<MixinTargetContract> softTargets() {
        return List.of(
                new MixinTargetContract(
                        "org.caecorthus.strawcraft.mixin.PlayerEntityMixin",
                        "net.minecraft.entity.player.PlayerEntity",
                        "dev.doctor4t.wathe.mixin.PlayerEntityMixin",
                        "wathe$overrideMovementSpeed",
                        "(F)F",
                        REQUIRED,
                        "Preserves vanilla movement speed plus map modifiers. 保留原版移动速度并叠加地图倍率。"
                ),
                new MixinTargetContract(
                        "org.caecorthus.strawcraft.mixin.PlayerEntityMixin",
                        "net.minecraft.entity.player.PlayerEntity",
                        "dev.doctor4t.wathe.mixin.PlayerEntityMixin",
                        "wathe$limitSprint",
                        "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                        REQUIRED,
                        "Disables Wathe stamina sprint limits. 禁用 Wathe 体力疾跑限制。"
                ),
                new MixinTargetContract(
                        "org.caecorthus.strawcraft.mixin.client.VanillaInventoryScreenMixin",
                        "net.minecraft.client.MinecraftClient",
                        "dev.doctor4t.wathe.mixin.client.restrictions.MinecraftClientMixin",
                        "wathe$replaceInventoryScreenWithLimitedInventoryScreen",
                        "(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)V",
                        REQUIRED,
                        "Keeps vanilla inventory instead of Wathe limited UI. 保持原版背包而不是 Wathe 限制界面。"
                )
        );
    }
}
