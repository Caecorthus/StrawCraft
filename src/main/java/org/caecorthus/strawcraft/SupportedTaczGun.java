package org.caecorthus.strawcraft;

import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public record SupportedTaczGun(String catalogId, TaczGunProfile profile) {
    public Identifier gunId() {
        return profile.gunId();
    }

    public Identifier ammoId() {
        return profile.ammoId();
    }

    public NbtComponent createGunCustomData() {
        return TaczGunStacks.createGunCustomData(profile);
    }

    public ItemStack createGunStack() {
        return TaczGunStacks.createGunStack(profile);
    }

    public NbtComponent createAmmoCustomData() {
        return TaczGunStacks.createAmmoCustomData(profile);
    }

    public ItemStack createAmmoStack(int count) {
        return TaczGunStacks.createAmmoStack(profile, count);
    }
}
