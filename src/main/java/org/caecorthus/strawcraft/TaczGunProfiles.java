package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;

public final class TaczGunProfiles {
    public static final TaczGunProfile RHINO357 = new TaczGunProfile(Identifier.of("tacz", "rhino357"), Identifier.of("tacz", "357mag"), 6, 48);
    public static final TaczGunProfile P320 = new TaczGunProfile(Identifier.of("tacz", "p320"), Identifier.of("tacz", "45acp"), 12, 60);

    private static final Map<Identifier, TaczGunProfile> PROFILES = Map.of(
            RHINO357.gunId(), RHINO357,
            P320.gunId(), P320
    );

    private TaczGunProfiles() {
    }

    public static Optional<TaczGunProfile> profileFor(Identifier gunId) {
        return Optional.ofNullable(PROFILES.get(gunId));
    }
}
