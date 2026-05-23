package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class StrawRoleMeaning {
    private StrawRoleMeaning() {
    }

    public static boolean canUseKillerShop(Role role) {
        return factionFor(role) == StrawFaction.KILLER;
    }

    public static Optional<GunAmmoFaction> ammoFactionFor(Role role) {
        return defaultAmmoFactionTags().resolveRole(role);
    }

    public static boolean receivesVigilanteLoadout(Role role) {
        return role != null && WatheRoleIds.VIGILANTE.equals(role.identifier());
    }

    public static StrawFaction factionFor(Role role) {
        if (role == null) {
            return StrawFaction.NONE;
        }
        Identifier roleId = role.identifier();
        if (WatheRoleIds.DISCOVERY_CIVILIAN.equals(roleId) || WatheRoleIds.NO_ROLE.equals(roleId)) {
            return StrawFaction.NONE;
        }
        if (WatheRoleIds.CIVILIAN.equals(roleId) || WatheRoleIds.VIGILANTE.equals(roleId)) {
            return StrawFaction.GOOD;
        }
        if (WatheRoleIds.KILLER.equals(roleId)) {
            return StrawFaction.KILLER;
        }
        if (WatheRoleIds.LOOSE_END.equals(roleId)) {
            return StrawFaction.NEUTRAL;
        }
        if (role.canUseKiller()) {
            return StrawFaction.KILLER;
        }
        if (role.isInnocent()) {
            return StrawFaction.GOOD;
        }
        return StrawFaction.NONE;
    }

    static GunAmmoFactionTags defaultAmmoFactionTags() {
        return GunAmmoFactionTags.empty()
                .withPoliceRole(WatheRoleIds.VIGILANTE);
    }

    static EnumSet<GunAmmoFaction> matchingAmmoFactions(
            Role role,
            Set<Identifier> policeRoles,
            Set<Identifier> civilianRoles,
            Set<Identifier> killerRoles
    ) {
        if (role == null || WatheRoleIds.DISCOVERY_CIVILIAN.equals(role.identifier())) {
            return EnumSet.noneOf(GunAmmoFaction.class);
        }

        EnumSet<GunAmmoFaction> matches = EnumSet.noneOf(GunAmmoFaction.class);
        Identifier roleId = role.identifier();
        if (policeRoles.contains(roleId)) {
            matches.add(GunAmmoFaction.POLICE);
        }
        StrawFaction faction = factionFor(role);
        if (killerRoles.contains(roleId) || faction == StrawFaction.KILLER) {
            matches.add(GunAmmoFaction.KILLER);
        }
        if (civilianRoles.contains(roleId)) {
            matches.add(GunAmmoFaction.CIVILIAN);
        } else if (faction == StrawFaction.GOOD && !matches.contains(GunAmmoFaction.POLICE) && !matches.contains(GunAmmoFaction.KILLER)) {
            // Civilian is the broad good-player fallback; special police/killer tags stay explicit.
            // Civilian 是好人阵营的宽泛兜底；警察、杀手这类特殊标签仍然显式匹配。
            matches.add(GunAmmoFaction.CIVILIAN);
        }

        return matches;
    }

    static String describeForLog(Role role) {
        if (role == null) {
            return "<none>";
        }
        return role.identifier().toString();
    }
}
