package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;

import java.util.Optional;

public final class StrawRoleMeaning {
    private StrawRoleMeaning() {
    }

    public enum RoleKind {
        UNKNOWN,
        KILLER,
        DETECTIVE,
        BYSTANDER
    }

    public record Meaning(Identifier roleId, StrawFaction faction, RoleKind kind) {
    }

    public static boolean canUseKillerShop(Role role) {
        return meaningFor(role).faction() == StrawFaction.KILLER;
    }

    public static Optional<GunAmmoFaction> ammoFactionFor(Role role) {
        return defaultAmmoFactionTags().resolveMeaning(meaningFor(role));
    }

    public static boolean receivesVigilanteLoadout(Role role) {
        return role != null && WatheRoleIds.VIGILANTE.equals(role.identifier());
    }

    public static StrawFaction factionFor(Role role) {
        return meaningFor(role).faction();
    }

    static Meaning meaningFor(Role role) {
        if (role == null) {
            return new Meaning(null, StrawFaction.NONE, RoleKind.UNKNOWN);
        }
        Identifier roleId = role.identifier();
        if (WatheRoleIds.DISCOVERY_CIVILIAN.equals(roleId) || WatheRoleIds.NO_ROLE.equals(roleId)) {
            return new Meaning(roleId, StrawFaction.NONE, RoleKind.UNKNOWN);
        }
        if (WatheRoleIds.VIGILANTE.equals(roleId)) {
            return new Meaning(roleId, StrawFaction.GOOD, RoleKind.DETECTIVE);
        }
        if (WatheRoleIds.CIVILIAN.equals(roleId)) {
            return new Meaning(roleId, StrawFaction.GOOD, RoleKind.BYSTANDER);
        }
        if (WatheRoleIds.KILLER.equals(roleId)) {
            return new Meaning(roleId, StrawFaction.KILLER, RoleKind.KILLER);
        }
        if (WatheRoleIds.LOOSE_END.equals(roleId)) {
            return new Meaning(roleId, StrawFaction.NEUTRAL, RoleKind.UNKNOWN);
        }
        Optional<NoellesRoleCatalog.Entry> noellesRole = NoellesRoleCatalog.find(roleId);
        if (noellesRole.isPresent()) {
            StrawFaction faction = noellesRole.get().faction();
            return new Meaning(roleId, faction, kindForNoellesFaction(faction));
        }
        if (role.canUseKiller()) {
            return new Meaning(roleId, StrawFaction.KILLER, RoleKind.KILLER);
        }
        if (role.isInnocent()) {
            return new Meaning(roleId, StrawFaction.GOOD, RoleKind.BYSTANDER);
        }
        return new Meaning(roleId, StrawFaction.NONE, RoleKind.UNKNOWN);
    }

    private static RoleKind kindForNoellesFaction(StrawFaction faction) {
        return switch (faction) {
            case KILLER -> RoleKind.KILLER;
            case GOOD -> RoleKind.BYSTANDER;
            case NONE, NEUTRAL, WITCH -> RoleKind.UNKNOWN;
        };
    }

    static GunAmmoFactionTags defaultAmmoFactionTags() {
        return GunAmmoFactionTags.empty()
                .withPoliceRole(WatheRoleIds.VIGILANTE);
    }

    static boolean deniesAmmoFaction(Identifier roleId) {
        return WatheRoleIds.DISCOVERY_CIVILIAN.equals(roleId);
    }

    static String describeForLog(Role role) {
        if (role == null) {
            return "<none>";
        }
        return role.identifier().toString();
    }
}
