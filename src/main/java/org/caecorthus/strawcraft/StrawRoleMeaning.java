package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class StrawRoleMeaning {
    private StrawRoleMeaning() {
    }

    public static boolean canUseKillerShop(Role role) {
        return role != null && role.canUseKiller();
    }

    public static Optional<GunAmmoFaction> ammoFactionFor(Role role) {
        return defaultAmmoFactionTags().resolveRole(role);
    }

    public static boolean receivesVigilanteLoadout(Role role) {
        return role != null && WatheRoleIds.VIGILANTE.equals(role.identifier());
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
        if (killerRoles.contains(roleId) || canUseKillerShop(role)) {
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

    static String describeForLog(Role role) {
        if (role == null) {
            return "<none>";
        }
        return role.identifier().toString();
    }
}
