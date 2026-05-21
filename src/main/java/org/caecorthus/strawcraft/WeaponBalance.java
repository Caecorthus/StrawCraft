package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public final class WeaponBalance {
    public static final float KNIFE_FRONT_STAB_DAMAGE = 8.0f;
    public static final float KNIFE_BACKSTAB_DAMAGE = 14.0f;
    public static final float KNIFE_ATTACK_DAMAGE = 4.0f;
    public static final float KNIFE_ATTACK_SPEED = 2.0f;
    public static final float BAT_ATTACK_DAMAGE = 12.0f;
    public static final float BAT_ATTACK_SPEED = 1.6f;

    private static final double BACKSTAB_ARC_COSINE = Math.cos(Math.toRadians(120.0));
    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    private WeaponBalance() {
    }

    public static void registerItemAttributes() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(WatheItems.KNIFE, builder -> builder.add(
                    DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    combatAttributes(KNIFE_ATTACK_DAMAGE, KNIFE_ATTACK_SPEED)
            ));
            context.modify(WatheItems.BAT, builder -> builder.add(
                    DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    combatAttributes(BAT_ATTACK_DAMAGE, BAT_ATTACK_SPEED)
            ));
        });
    }

    public static boolean isDisabledWatheGun(ItemStack stack) {
        return stack.isOf(WatheItems.REVOLVER) || stack.isOf(WatheItems.DERRINGER);
    }

    public static boolean isBackstab(PlayerEntity victim, PlayerEntity attacker) {
        Vec3d victimLook = victim.getRotationVec(1.0f);
        Vec3d victimToAttacker = attacker.getPos().subtract(victim.getPos());
        return isInsideBackstabArc(victimLook, victimToAttacker);
    }

    public static boolean isInsideBackstabArc(Vec3d targetLook, Vec3d targetToAttacker) {
        double lookLength = horizontalLength(targetLook);
        double attackerLength = horizontalLength(targetToAttacker);
        if (lookLength <= 1.0E-6 || attackerLength <= 1.0E-6) {
            return false;
        }

        // The 120 degree backstab cone is centered on the victim's back,
        // so the attacker must be at least 120 degrees away from the victim's look vector.
        // 120 度背刺判定锥以受害者背后为中心，
        // 所以攻击者和受害者视线方向的夹角至少要达到 120 度。
        double dot = (targetLook.x * targetToAttacker.x + targetLook.z * targetToAttacker.z) / (lookLength * attackerLength);
        return dot <= BACKSTAB_ARC_COSINE;
    }

    private static AttributeModifiersComponent combatAttributes(float attackDamage, float attackSpeed) {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                                attackDamage - PLAYER_BASE_ATTACK_DAMAGE,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(
                                Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                                attackSpeed - PLAYER_BASE_ATTACK_SPEED,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.MAINHAND)
                .build();
    }

    private static double horizontalLength(Vec3d vector) {
        return Math.sqrt(vector.x * vector.x + vector.z * vector.z);
    }
}
