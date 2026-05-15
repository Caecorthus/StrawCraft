package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

public record TaczGunProfile(
        Identifier gunId,
        Identifier ammoId,
        int maxMagazineAmmo,
        int ammoMaxStackSize
) {
    public boolean isLowAmmo(int currentAmmo) {
        return currentAmmo <= Math.floorDiv(maxMagazineAmmo, 3);
    }

    public int missingAmmo(int currentAmmo) {
        return Math.max(0, maxMagazineAmmo - currentAmmo);
    }
}
