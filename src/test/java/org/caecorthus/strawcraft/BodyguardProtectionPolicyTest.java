package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import org.caecorthus.strawcraft.api.StrawKillEvents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyguardProtectionPolicyTest {
    private static final UUID VICTIM = new UUID(0, 10);
    private static final UUID NEAR_BODYGUARD = new UUID(0, 20);
    private static final UUID FAR_BODYGUARD = new UUID(0, 30);

    @Test
    void nearbyBodyguardProtectsAnotherAliveVictimAndConsumesOneCharge() {
        NoellesRoleState state = chargedState();

        StrawKillEvents.KillDecision decision = BodyguardProtectionPolicy.beforeKill(input(
                true,
                true,
                GameConstants.DeathReasons.KNIFE,
                candidate(NEAR_BODYGUARD, "bodyguard", true, 4.0, state)
        ));

        assertTrue(decision.cancelWatheKill());
        assertFalse(state.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
    }

    @Test
    void nearestBodyguardWinsWithUuidTieBreak() {
        NoellesRoleState farther = chargedState();
        NoellesRoleState fartherTie = chargedState();
        NoellesRoleState nearestHighUuid = chargedState();
        NoellesRoleState nearestLowUuid = chargedState();
        UUID highUuid = new UUID(0, 50);
        UUID lowUuid = new UUID(0, 40);

        StrawKillEvents.KillDecision decision = BodyguardProtectionPolicy.beforeKill(input(
                true,
                true,
                GameConstants.DeathReasons.GUN,
                candidate(FAR_BODYGUARD, "bodyguard", true, 9.0, farther),
                candidate(new UUID(0, 60), "bodyguard", true, 9.0, fartherTie),
                candidate(highUuid, "bodyguard", true, 4.0, nearestHighUuid),
                candidate(lowUuid, "bodyguard", true, 4.0, nearestLowUuid)
        ));

        assertTrue(decision.cancelWatheKill());
        assertTrue(farther.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertTrue(fartherTie.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertTrue(nearestHighUuid.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertFalse(nearestLowUuid.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
    }

    @Test
    void denialCasesDoNotCancelOrConsumeCharge() {
        NoellesRoleState self = chargedState();
        NoellesRoleState wrongRole = chargedState();
        NoellesRoleState dead = chargedState();
        NoellesRoleState outOfRange = chargedState();
        NoellesRoleState noCharge = new NoellesRoleState();
        BodyguardProtectionPolicy.Candidate eligibleSelf = candidate(VICTIM, "bodyguard", true, 1.0, self);
        BodyguardProtectionPolicy.Candidate eligibleWrongRole = candidate(NEAR_BODYGUARD, "detective", true, 1.0, wrongRole);
        BodyguardProtectionPolicy.Candidate eligibleDead = candidate(NEAR_BODYGUARD, "bodyguard", false, 1.0, dead);
        BodyguardProtectionPolicy.Candidate eligibleOutOfRange = candidate(
                NEAR_BODYGUARD,
                "bodyguard",
                true,
                BodyguardProtectionPolicy.PROTECTION_RANGE_SQUARED + 1.0,
                outOfRange
        );
        BodyguardProtectionPolicy.Candidate eligibleNoCharge = candidate(NEAR_BODYGUARD, "bodyguard", true, 1.0, noCharge);

        assertPasses(input(true, true, GameConstants.DeathReasons.KNIFE, eligibleSelf));
        assertPasses(input(true, true, GameConstants.DeathReasons.KNIFE, eligibleWrongRole));
        assertPasses(input(true, true, GameConstants.DeathReasons.KNIFE, eligibleDead));
        assertPasses(input(true, true, GameConstants.DeathReasons.KNIFE, eligibleOutOfRange));
        assertPasses(input(true, true, GameConstants.DeathReasons.KNIFE, eligibleNoCharge));
        assertPasses(input(true, true, StrawDeathReasons.SHOT_INNOCENT, candidate(NEAR_BODYGUARD, "bodyguard", true, 1.0, chargedState())));
        assertPasses(input(true, true, StrawDeathReasons.ASSASSINATED, candidate(NEAR_BODYGUARD, "bodyguard", true, 1.0, chargedState())));
        assertPasses(input(false, true, GameConstants.DeathReasons.KNIFE, candidate(NEAR_BODYGUARD, "bodyguard", true, 1.0, chargedState())));
        assertPasses(input(true, false, GameConstants.DeathReasons.KNIFE, candidate(NEAR_BODYGUARD, "bodyguard", true, 1.0, chargedState())));

        assertTrue(self.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertTrue(wrongRole.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertTrue(dead.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertTrue(outOfRange.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
        assertFalse(noCharge.hasFlag(BodyguardProtectionPolicy.PROTECTION_FLAG));
    }

    @Test
    void duplicateKillPassesAfterOneChargeIsConsumed() {
        NoellesRoleState state = chargedState();
        BodyguardProtectionPolicy.Input input = input(
                true,
                true,
                GameConstants.DeathReasons.KNIFE,
                candidate(NEAR_BODYGUARD, "bodyguard", true, 4.0, state)
        );

        assertTrue(BodyguardProtectionPolicy.beforeKill(input).cancelWatheKill());
        assertFalse(BodyguardProtectionPolicy.beforeKill(input).cancelWatheKill());
    }

    @Test
    void assignmentPlanGrantsBodyguardStateOnly() {
        RoleAssignedLoadouts.AssignmentPlan bodyguardPlan = RoleAssignedLoadouts.planAssignedLoadout(
                NoellesRoleCatalog.find(StrawCraft.id("bodyguard")).orElseThrow().watheRole(),
                false
        );
        RoleAssignedLoadouts.AssignmentPlan professorPlan = RoleAssignedLoadouts.planAssignedLoadout(
                NoellesRoleCatalog.find(StrawCraft.id("professor")).orElseThrow().watheRole(),
                false
        );

        assertTrue(bodyguardPlan.grantBodyguardProtection());
        assertFalse(bodyguardPlan.grantProfessorIronManProtection());
        assertEquals(List.of(), bodyguardPlan.assignmentItemGrants());
        assertEquals(List.of(), bodyguardPlan.unsupportedItemGrants());
        assertFalse(professorPlan.grantBodyguardProtection());
    }

    private static NoellesRoleState chargedState() {
        NoellesRoleState state = new NoellesRoleState();
        BodyguardProtectionPolicy.grant(state, 100L);
        return state;
    }

    private static void assertPasses(BodyguardProtectionPolicy.Input input) {
        assertFalse(BodyguardProtectionPolicy.beforeKill(input).cancelWatheKill());
    }

    private static BodyguardProtectionPolicy.Input input(
            boolean gameRunning,
            boolean victimAlive,
            net.minecraft.util.Identifier deathReason,
            BodyguardProtectionPolicy.Candidate... candidates
    ) {
        return new BodyguardProtectionPolicy.Input(VICTIM, gameRunning, victimAlive, deathReason, List.of(candidates));
    }

    private static BodyguardProtectionPolicy.Candidate candidate(
            UUID uuid,
            String rolePath,
            boolean alive,
            double squaredDistance,
            NoellesRoleState state
    ) {
        return new BodyguardProtectionPolicy.Candidate(
                uuid,
                role(StrawCraft.id(rolePath), true, false),
                alive,
                squaredDistance,
                BodyguardProtectionPolicy.charge(state)
        );
    }

    private static Role role(net.minecraft.util.Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
