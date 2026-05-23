package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class GunAmmoFactionTags {
    private static final GunAmmoFactionTags DEFAULT = StrawRoleMeaning.defaultAmmoFactionTags();

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
        return DEFAULT.isAmbiguousRole(role);
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
        return resolveMeaning(StrawRoleMeaning.meaningFor(role));
    }

    Optional<GunAmmoFaction> resolveMeaning(StrawRoleMeaning.Meaning meaning) {
        return RoleSemantics.ammoFactionFor(meaning.kind(), explicitFactionTags(meaning.roleId()));
    }

    public boolean isAmbiguousRole(Role role) {
        return matchingFactions(StrawRoleMeaning.meaningFor(role)).size() > 1;
    }

    private EnumSet<GunAmmoFaction> matchingFactions(StrawRoleMeaning.Meaning meaning) {
        return RoleSemantics.matchingAmmoFactions(meaning.kind(), explicitFactionTags(meaning.roleId()));
    }

    private EnumSet<GunAmmoFaction> explicitFactionTags(Identifier roleId) {
        EnumSet<GunAmmoFaction> tags = EnumSet.noneOf(GunAmmoFaction.class);
        if (roleId == null || StrawRoleMeaning.deniesAmmoFaction(roleId)) {
            return tags;
        }
        if (policeRoles.contains(roleId)) {
            tags.add(GunAmmoFaction.POLICE);
        }
        if (civilianRoles.contains(roleId)) {
            tags.add(GunAmmoFaction.CIVILIAN);
        }
        if (killerRoles.contains(roleId)) {
            tags.add(GunAmmoFaction.KILLER);
        }
        return tags;
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
