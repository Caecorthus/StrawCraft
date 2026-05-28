package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesNeutralWinPolicyTest {
    @Test
    void jesterOfficialDeathRecordsDeferredNeutralWinClaim() {
        NoellesRoleState state = new NoellesRoleState();
        UUID killer = UUID.randomUUID();

        boolean recorded = NoellesNeutralWinPolicy.recordOfficialDeathSideEffects(
                state,
                Optional.of(StrawCraft.id("jester")),
                officialDeath(Optional.of(killer), GameConstants.DeathReasons.GUN)
        );

        assertTrue(recorded);
        NoellesRoleState.NeutralWinClaim claim = state.neutralWinClaim(StrawCraft.id("jester")).orElseThrow();
        assertEquals(StrawCraft.id("jester"), claim.roleId());
        assertEquals(NoellesNeutralWinPolicy.JESTER_KILLED_TRIGGER, claim.trigger());
        assertEquals(Optional.of(killer), claim.opponentUuid());
        assertEquals(300L, claim.gameTime());
    }

    @Test
    void jesterDeathWithoutKillerDoesNotClaimNeutralWin() {
        NoellesRoleState state = new NoellesRoleState();

        boolean recorded = NoellesNeutralWinPolicy.recordOfficialDeathSideEffects(
                state,
                Optional.of(StrawCraft.id("jester")),
                officialDeath(Optional.empty(), GameConstants.DeathReasons.GENERIC)
        );

        assertFalse(recorded);
        assertTrue(state.neutralWinClaims().isEmpty());
    }

    @Test
    void nonNeutralRoleDeathDoesNotRecordNeutralWinState() {
        NoellesRoleState state = new NoellesRoleState();

        boolean recorded = NoellesNeutralWinPolicy.recordOfficialDeathSideEffects(
                state,
                Optional.of(WatheRoleIds.CIVILIAN),
                officialDeath(Optional.of(UUID.randomUUID()), GameConstants.DeathReasons.GUN)
        );

        assertFalse(recorded);
        assertTrue(state.neutralWinClaims().isEmpty());
    }

    @Test
    void recordedNeutralClaimDoesNotOverrideOfficialWinnerSemantics() {
        UUID jester = UUID.randomUUID();
        NoellesRoleState state = new NoellesRoleState();
        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                StrawCraft.id("jester"),
                NoellesNeutralWinPolicy.JESTER_KILLED_TRIGGER,
                Optional.of(UUID.randomUUID()),
                300L
        ));

        StrawWinEvents.WinContribution contribution =
                NoellesNeutralWinPolicy.contributeRecordedNeutralWins(jester, state);

        assertFalse(contribution.suppressDefaultWin());
        assertEquals(Optional.empty(), contribution.replacementDefaultWin());
        assertTrue(contribution.extraWinners().isEmpty());
    }

    @Test
    void stateWithoutNeutralClaimLeavesDefaultWinUntouched() {
        StrawWinEvents.WinContribution contribution =
                NoellesNeutralWinPolicy.contributeRecordedNeutralWins(UUID.randomUUID(), new NoellesRoleState());

        assertFalse(contribution.suppressDefaultWin());
        assertTrue(contribution.extraWinners().isEmpty());
    }

    private static StrawDeathEvents.OfficialDeathContext officialDeath(Optional<UUID> killer, Identifier deathReason) {
        return new StrawDeathEvents.OfficialDeathContext(
                Identifier.of("minecraft", "overworld"),
                UUID.randomUUID(),
                killer,
                false,
                deathReason,
                300L,
                true,
                true
        );
    }
}
