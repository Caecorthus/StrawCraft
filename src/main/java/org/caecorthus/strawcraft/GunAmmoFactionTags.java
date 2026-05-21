package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class GunAmmoFactionTags {
    private static final GunAmmoFactionTags DEFAULT = GunAmmoFactionTags.empty()
            .withPoliceRole(WatheRoleIds.VIGILANTE);

    private final Set<Identifier> policeRoles;
    private final Set<Identifier> civilianRoles;
    private final Set<Identifier> killerRoles;

    private GunAmmoFactionTags(Set<Identifier> policeRoles, Set<Identifier> civilianRoles, Set<Identifier> killerRoles) {
        this.policeRoles = Set.copyOf(policeRoles);
        this.civilianRoles = Set.copyOf(civilianRoles);
        this.killerRoles = Set.copyOf(killerRoles);
    }

    public static GunAmmoFactionTags empty() {
        return new GunAmmoFactionTags(Set.of(), Set.of(), Set.of());
    }

    public static Optional<GunAmmoFaction> resolve(Role role) {
        return DEFAULT.resolveRole(role);
    }

    public static boolean isAmbiguous(Role role) {
        return DEFAULT.matchingFactions(role).size() > 1;
    }

    public GunAmmoFactionTags withPoliceRole(Identifier roleId) {
        return copyWith(roleId, GunAmmoFaction.POLICE);
    }

    public GunAmmoFactionTags withCivilianRole(Identifier roleId) {
        return copyWith(roleId, GunAmmoFaction.CIVILIAN);
    }

    public GunAmmoFactionTags withKillerRole(Identifier roleId) {
        return copyWith(roleId, GunAmmoFaction.KILLER);
    }

    public Optional<GunAmmoFaction> resolveRole(Role role) {
        EnumSet<GunAmmoFaction> matches = matchingFactions(role);
        if (matches.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(matches.iterator().next());
    }

    public boolean isAmbiguousRole(Role role) {
        return matchingFactions(role).size() > 1;
    }

    private EnumSet<GunAmmoFaction> matchingFactions(Role role) {
        if (role == null || WatheRoleIds.DISCOVERY_CIVILIAN.equals(role.identifier())) {
            return EnumSet.noneOf(GunAmmoFaction.class);
        }

        EnumSet<GunAmmoFaction> matches = EnumSet.noneOf(GunAmmoFaction.class);
        Identifier roleId = role.identifier();
        if (policeRoles.contains(roleId)) {
            matches.add(GunAmmoFaction.POLICE);
        }
        if (killerRoles.contains(roleId) || role.canUseKiller()) {
            matches.add(GunAmmoFaction.KILLER);
        }
        if (civilianRoles.contains(roleId)) {
            matches.add(GunAmmoFaction.CIVILIAN);
        } else if (role.isInnocent() && !matches.contains(GunAmmoFaction.POLICE) && !matches.contains(GunAmmoFaction.KILLER)) {
            // Civilian is the broad good-player fallback; special police/killer tags stay explicit.
            // Civilian 是好人阵营的宽泛兜底；警察、杀手这类特殊标签仍然显式匹配。
            matches.add(GunAmmoFaction.CIVILIAN);
        }

        return matches;
    }

    private GunAmmoFactionTags copyWith(Identifier roleId, GunAmmoFaction faction) {
        Set<Identifier> nextPolice = new HashSet<>(policeRoles);
        Set<Identifier> nextCivilian = new HashSet<>(civilianRoles);
        Set<Identifier> nextKiller = new HashSet<>(killerRoles);
        switch (faction) {
            case POLICE -> nextPolice.add(roleId);
            case CIVILIAN -> nextCivilian.add(roleId);
            case KILLER -> nextKiller.add(roleId);
        }
        return new GunAmmoFactionTags(nextPolice, nextCivilian, nextKiller);
    }
}
