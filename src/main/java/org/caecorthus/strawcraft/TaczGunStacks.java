package org.caecorthus.strawcraft;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

public final class TaczGunStacks {
    public static final Identifier MODERN_KINETIC_GUN_ITEM_ID = Identifier.of("tacz", "modern_kinetic_gun");
    public static final Identifier AMMO_ITEM_ID = Identifier.of("tacz", "ammo");
    public static final String GUN_ID_TAG = "GunId";
    public static final String GUN_CURRENT_AMMO_COUNT_TAG = "GunCurrentAmmoCount";
    public static final String AMMO_ID_TAG = "AmmoId";
    public static final String STRAWCRAFT_AMMO_CYCLE_ID_TAG = "StrawCraftAmmoCycleId";

    private TaczGunStacks() {
    }

    private static NbtComponent createGunCustomData(Identifier gunId) {
        NbtCompound customData = new NbtCompound();
        customData.putString(GUN_ID_TAG, gunId.toString());
        return NbtComponent.of(customData);
    }

    public static NbtComponent createGunCustomData(TaczGunProfile profile) {
        return createGunCustomData(profile.gunId());
    }

    public static ItemStack createGunStack(TaczGunProfile profile) {
        Item item = Registries.ITEM.get(MODERN_KINETIC_GUN_ITEM_ID);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        // TACZ identifies individual guns through custom data on tacz:modern_kinetic_gun.
        stack.set(DataComponentTypes.CUSTOM_DATA, createGunCustomData(profile));
        return stack;
    }

    public static NbtComponent createAmmoCustomData(TaczGunProfile profile) {
        return createAmmoCustomData(profile.ammoId());
    }

    private static NbtComponent createAmmoCustomData(Identifier ammoId) {
        NbtCompound customData = new NbtCompound();
        customData.putString(AMMO_ID_TAG, ammoId.toString());
        return NbtComponent.of(customData);
    }

    public static ItemStack createAmmoStack(TaczGunProfile profile, int count) {
        Item item = Registries.ITEM.get(AMMO_ITEM_ID);
        if (item == Items.AIR || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponentTypes.CUSTOM_DATA, createAmmoCustomData(profile));
        stack.set(DataComponentTypes.MAX_STACK_SIZE, profile.ammoMaxStackSize());
        return stack;
    }

    public static Optional<Identifier> getGunId(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();
        if (!nbt.contains(GUN_ID_TAG, NbtElement.STRING_TYPE)) {
            return Optional.empty();
        }
        return Optional.ofNullable(Identifier.tryParse(nbt.getString(GUN_ID_TAG)));
    }

    public static int getCurrentAmmo(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!nbt.contains(GUN_CURRENT_AMMO_COUNT_TAG, NbtElement.INT_TYPE)) {
            return 0;
        }
        return Math.max(0, nbt.getInt(GUN_CURRENT_AMMO_COUNT_TAG));
    }

    public static void setAmmoCycleId(ItemStack stack, UUID cycleId) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putUuid(STRAWCRAFT_AMMO_CYCLE_ID_TAG, cycleId);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static Optional<UUID> getAmmoCycleId(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!nbt.containsUuid(STRAWCRAFT_AMMO_CYCLE_ID_TAG)) {
            return Optional.empty();
        }
        return Optional.of(nbt.getUuid(STRAWCRAFT_AMMO_CYCLE_ID_TAG));
    }
}
