package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.api.Role;
import org.caecorthus.strawcraft.api.StrawKillEvents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessorIronManProtectionTest {
    @Test
    void ironManProtectionCancelsOneEligibleKillThenClearsTheFlag() {
        NoellesRoleState state = new NoellesRoleState();
        ProfessorIronManProtection.grant(state, 100L);

        assertTrue(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
        assertFalse(state.hasFlag(ProfessorIronManProtection.PROTECTION_FLAG));
        assertFalse(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
    }

    @Test
    void duplicateAssignmentDoesNotRearmProtectionAfterTheOneRoundGrantWasConsumed() {
        NoellesRoleState state = new NoellesRoleState();
        ProfessorIronManProtection.grant(state, 100L);

        assertTrue(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
        ProfessorIronManProtection.grant(state, 200L);

        assertFalse(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
        assertEquals(100L, state.getTimestamp(ProfessorIronManProtection.GRANTED_AT_TIMESTAMP).orElseThrow());
    }

    @Test
    void roundScopedStateResetAllowsTheNextRoundToGrantOneNewProtection() {
        NoellesRoleState state = new NoellesRoleState();
        ProfessorIronManProtection.grant(state, 100L);
        assertTrue(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());

        state.reset();
        ProfessorIronManProtection.grant(state, 300L);

        assertTrue(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
        assertFalse(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
    }

    @Test
    void missingProtectionFlagDoesNotCancelKill() {
        NoellesRoleState state = new NoellesRoleState();

        assertFalse(ProfessorIronManProtection.beforeKill(state, GameConstants.DeathReasons.KNIFE).cancelWatheKill());
    }

    @Test
    void nonProfessorRoleWithProtectionFlagStillDoesNotCancelOrConsumeKills() {
        NoellesRoleState state = new NoellesRoleState();
        ProfessorIronManProtection.grant(state, 100L);

        assertFalse(ProfessorIronManProtection.beforeKill(
                role(StrawCraft.id("detective"), true, false),
                state,
                GameConstants.DeathReasons.KNIFE
        ).cancelWatheKill());
        assertTrue(state.hasFlag(ProfessorIronManProtection.PROTECTION_FLAG));
    }

    @Test
    void specialPunishmentAndAssassinationDeathsDoNotConsumeProtection() {
        NoellesRoleState state = new NoellesRoleState();
        ProfessorIronManProtection.grant(state, 100L);

        assertFalse(ProfessorIronManProtection.beforeKill(state, StrawDeathReasons.SHOT_INNOCENT).cancelWatheKill());
        assertFalse(ProfessorIronManProtection.beforeKill(state, StrawDeathReasons.ASSASSINATED).cancelWatheKill());
        assertTrue(state.hasFlag(ProfessorIronManProtection.PROTECTION_FLAG));
    }

    @Test
    void professorProtectionUsesExistingKillDecisionContract() {
        NoellesRoleState state = new NoellesRoleState();
        ProfessorIronManProtection.grant(state, 100L);

        StrawKillEvents.KillDecision decision = ProfessorIronManProtection.beforeKill(
                role(StrawCraft.id("professor"), true, false),
                state,
                GameConstants.DeathReasons.GUN
        );

        assertTrue(decision.cancelWatheKill());
    }

    @Test
    void professorAssignmentPlansExactlyOneStateGrantAndNoFakeVialItem() {
        RoleAssignedLoadouts.AssignmentPlan professorPlan = RoleAssignedLoadouts.planAssignedLoadout(
                NoellesRoleCatalog.find(StrawCraft.id("professor")).orElseThrow().watheRole(),
                false
        );
        RoleAssignedLoadouts.AssignmentPlan detectivePlan = RoleAssignedLoadouts.planAssignedLoadout(
                NoellesRoleCatalog.find(StrawCraft.id("detective")).orElseThrow().watheRole(),
                false
        );

        assertTrue(professorPlan.grantProfessorIronManProtection());
        assertEquals(List.of(), professorPlan.assignmentItemGrants());
        assertEquals(List.of(), professorPlan.unsupportedItemGrants());
        assertFalse(detectivePlan.grantProfessorIronManProtection());
    }

    private static Role role(net.minecraft.util.Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
