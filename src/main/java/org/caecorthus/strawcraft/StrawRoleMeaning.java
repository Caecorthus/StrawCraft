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

    public static boolean usesBomberShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("bomber"));
    }

    public static boolean usesScavengerShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("scavenger"));
    }

    public static boolean usesTimekeeperShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("timekeeper"));
    }

    public static boolean usesReporterShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("reporter"));
    }

    public static boolean usesBartenderShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("bartender"));
    }

    public static boolean usesWaiterShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("waiter"));
    }

    public static boolean usesSilencerShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("silencer"));
    }

    public static boolean usesPoisonerShop(Role role) {
        return matchesRoleId(role, StrawCraft.id("poisoner"));
    }

    public static Optional<GunAmmoFaction> ammoFactionFor(Role role) {
        return defaultAmmoFactionTags().resolveMeaning(meaningFor(role));
    }

    public static boolean receivesVigilanteLoadout(Role role) {
        return role != null && WatheRoleIds.VIGILANTE.equals(role.identifier());
    }

    public static boolean receivesUndercoverWalkieTalkie(Role role) {
        return role != null && StrawCraft.id("undercover").equals(role.identifier());
    }

    public static boolean receivesDetectiveInvestigation(Role role) {
        return role != null && StrawCraft.id("detective").equals(role.identifier());
    }

    public static boolean receivesCoronerInspection(Role role) {
        return role != null && CoronerInspectionPolicy.CORONER_ROLE.equals(role.identifier());
    }

    public static boolean receivesProfessorIronManProtection(Role role) {
        return role != null && StrawCraft.id("professor").equals(role.identifier());
    }

    public static boolean receivesBodyguardProtection(Role role) {
        return role != null && BodyguardProtectionPolicy.BODYGUARD_ROLE.equals(role.identifier());
    }

    public static boolean receivesVultureBodyFeast(Role role) {
        return role != null && VultureBodyFeastPolicy.VULTURE_ROLE.equals(role.identifier());
    }

    public static boolean receivesSurvivalMasterCountdown(Role role) {
        return role != null && StrawCraft.id("survival_master").equals(role.identifier());
    }

    public static boolean receivesPoisonVisibility(Role role) {
        // 为理解毒饮的职业复用 Wathe 官方毒药可视钩子。
        // 为理解毒饮的职业复用 Wathe 官方毒药可视钩子。
        // 为理解毒饮的职业复用 Wathe 官方毒药可视钩子。
        return role != null && (StrawCraft.id("toxicologist").equals(role.identifier())
                || StrawCraft.id("bartender").equals(role.identifier())
                || usesPoisonerShop(role));
    }

    public static boolean receivesToxicologistPoisonVisibility(Role role) {
        return role != null && (StrawCraft.id("toxicologist").equals(role.identifier())
                || usesPoisonerShop(role));
    }

    public static boolean receivesAttendantRoomManifest(Role role) {
        return role != null && StrawCraft.id("attendant").equals(role.identifier());
    }

    public static boolean receivesRecallerRecall(Role role) {
        return role != null && StrawCraft.id("recaller").equals(role.identifier());
    }

    public static boolean receivesSwapperSwap(Role role) {
        return role != null && StrawCraft.id("swapper").equals(role.identifier());
    }

    public static boolean receivesReporterMark(Role role) {
        return role != null && StrawCraft.id("reporter").equals(role.identifier());
    }

    public static boolean receivesVoodooDeathBond(Role role) {
        return role != null && VoodooBondPolicy.VOODOO_ROLE.equals(role.identifier());
    }

    public static boolean receivesPhantomInvisibility(Role role) {
        return role != null && PhantomInvisibilityPolicy.PHANTOM_ROLE.equals(role.identifier());
    }

    public static boolean receivesDemonHunterPsychoResponse(Role role) {
        return role != null && DemonHunterPsychoPolicy.DEMON_HUNTER_ROLE.equals(role.identifier());
    }

    public static boolean receivesMermaidWaterAdaptation(Role role) {
        return role != null && MermaidWaterAdaptationPolicy.MERMAID_ROLE.equals(role.identifier());
    }

    static boolean matchesRoleId(Role role, Identifier roleId) {
        return role != null && roleId.equals(role.identifier());
    }

    public static StrawFaction factionFor(Role role) {
        return meaningFor(role).faction();
    }

    public static Optional<Identifier> roleIdFor(Role role) {
        return role == null ? Optional.empty() : Optional.of(role.identifier());
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
