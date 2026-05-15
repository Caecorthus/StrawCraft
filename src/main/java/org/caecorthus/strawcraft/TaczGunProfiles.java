package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;

public final class TaczGunProfiles {
    private static final Map<Identifier, TaczGunProfile> PROFILES = Map.of(
            Identifier.of("tacz", "rhino357"),
            new TaczGunProfile(Identifier.of("tacz", "rhino357"), Identifier.of("tacz", "357mag"), 6, 48),
            Identifier.of("tacz", "p320"),
            new TaczGunProfile(Identifier.of("tacz", "p320"), Identifier.of("tacz", "45acp"), 12, 60)
    );

    private TaczGunProfiles() {
    }

    public static Optional<TaczGunProfile> profileFor(Identifier gunId) {
        return Optional.ofNullable(PROFILES.get(gunId));
    }
}
