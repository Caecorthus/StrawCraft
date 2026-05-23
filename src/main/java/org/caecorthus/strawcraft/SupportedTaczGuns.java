package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;

public final class SupportedTaczGuns {
    public static final SupportedTaczGun P320 = new SupportedTaczGun("p320", TaczGunProfiles.P320);

    private static final Map<Identifier, SupportedTaczGun> GUNS = Map.of(
            P320.gunId(), P320
    );

    private SupportedTaczGuns() {
    }

    public static Optional<SupportedTaczGun> gunFor(Identifier gunId) {
        return Optional.ofNullable(GUNS.get(gunId));
    }
}
