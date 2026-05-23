package org.caecorthus.strawcraft;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class RoleSemantics {
    private RoleSemantics() {
    }

    public static Optional<GunAmmoFaction> ammoFactionFor(StrawRoleMeaning.RoleKind roleKind) {
        return exactlyOne(matchingAmmoFactions(roleKind, Set.of()));
    }

    static Optional<GunAmmoFaction> ammoFactionFor(
            StrawRoleMeaning.RoleKind roleKind,
            Set<GunAmmoFaction> explicitFactionTags
    ) {
        return exactlyOne(matchingAmmoFactions(roleKind, explicitFactionTags));
    }

    static EnumSet<GunAmmoFaction> matchingAmmoFactions(
            StrawRoleMeaning.RoleKind roleKind,
            Set<GunAmmoFaction> explicitFactionTags
    ) {
        EnumSet<GunAmmoFaction> matches = EnumSet.noneOf(GunAmmoFaction.class);
        matches.addAll(explicitFactionTags);

        switch (roleKind) {
            case KILLER -> matches.add(GunAmmoFaction.KILLER);
            case DETECTIVE -> matches.add(GunAmmoFaction.POLICE);
            case BYSTANDER -> {
                if (!matches.contains(GunAmmoFaction.POLICE) && !matches.contains(GunAmmoFaction.KILLER)) {
                    matches.add(GunAmmoFaction.CIVILIAN);
                }
            }
            case UNKNOWN -> {
            }
        }

        return matches;
    }

    private static Optional<GunAmmoFaction> exactlyOne(EnumSet<GunAmmoFaction> matches) {
        if (matches.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(matches.iterator().next());
    }
}
