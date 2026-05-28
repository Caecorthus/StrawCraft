package org.caecorthus.strawcraft;

import net.minecraft.nbt.NbtCompound;
import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VultureBodyFeastPolicyTest {
    @Test
    void bodiesRequiredUsesHalfTheRoundWithMinimumOneBody() {
        assertEquals(1, VultureBodyFeastPolicy.bodiesRequiredFor(1));
        assertEquals(1, VultureBodyFeastPolicy.bodiesRequiredFor(2));
        assertEquals(2, VultureBodyFeastPolicy.bodiesRequiredFor(5));
    }

    @Test
    void eatingFirstUniqueBodyIncrementsProgress() {
        NoellesRoleState state = new NoellesRoleState();
        VultureBodyFeastPolicy.resetRoundState(state, 4);

        VultureBodyFeastPolicy.FeastResult result =
                VultureBodyFeastPolicy.recordSuccessfulFeast(state, UUID.randomUUID(), 120L);

        assertTrue(result.accepted());
        assertEquals(1, VultureBodyFeastPolicy.bodiesEaten(state));
        assertEquals(2, VultureBodyFeastPolicy.bodiesRequired(state));
        assertTrue(state.neutralWinClaims().isEmpty());
    }

    @Test
    void duplicateBodyDoesNotIncrementTwice() {
        UUID body = UUID.randomUUID();
        NoellesRoleState state = new NoellesRoleState();
        VultureBodyFeastPolicy.resetRoundState(state, 4);

        VultureBodyFeastPolicy.recordSuccessfulFeast(state, body, 120L);
        VultureBodyFeastPolicy.FeastResult duplicate =
                VultureBodyFeastPolicy.recordSuccessfulFeast(state, body, 140L);

        assertFalse(duplicate.accepted());
        assertTrue(duplicate.duplicateBody());
        assertEquals(1, VultureBodyFeastPolicy.bodiesEaten(state));
    }

    @Test
    void thresholdRecordsVultureNeutralWinClaim() {
        NoellesRoleState state = new NoellesRoleState();
        VultureBodyFeastPolicy.resetRoundState(state, 4);

        VultureBodyFeastPolicy.recordSuccessfulFeast(state, UUID.randomUUID(), 120L);
        VultureBodyFeastPolicy.FeastResult winningFeast =
                VultureBodyFeastPolicy.recordSuccessfulFeast(state, UUID.randomUUID(), 180L);

        assertTrue(winningFeast.accepted());
        assertTrue(winningFeast.won());
        NoellesRoleState.NeutralWinClaim claim =
                state.neutralWinClaim(StrawCraft.id("vulture")).orElseThrow();
        assertEquals(StrawCraft.id("vulture"), claim.roleId());
        assertEquals(VultureBodyFeastPolicy.BODY_FEAST_TRIGGER, claim.trigger());
        assertEquals(Optional.empty(), claim.opponentUuid());
        assertEquals(180L, claim.gameTime());

        UUID vulture = UUID.randomUUID();
        StrawWinEvents.WinContribution contribution =
                NoellesNeutralWinPolicy.contributeRecordedNeutralWins(vulture, state);
        assertFalse(contribution.suppressDefaultWin());
        assertEquals(Optional.of(StrawWinEvents.DefaultWin.LOOSE_END), contribution.replacementDefaultWin());
        assertEquals(1, contribution.extraWinners().size());
        StrawWinEvents.ExtraWinner winner = contribution.extraWinners().iterator().next();
        assertEquals(vulture, winner.playerUuid());
        assertEquals(VultureBodyFeastPolicy.VULTURE_ROLE, winner.roleId());
        assertEquals(VultureBodyFeastPolicy.BODY_FEAST_TRIGGER, winner.triggerId());
    }

    @Test
    void resetAndNbtRoundTripPreserveVultureProgress() {
        UUID body = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        VultureBodyFeastPolicy.resetRoundState(saved, 6);
        VultureBodyFeastPolicy.recordSuccessfulFeast(saved, body, 120L);

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertEquals(1, VultureBodyFeastPolicy.bodiesEaten(loaded));
        assertEquals(3, VultureBodyFeastPolicy.bodiesRequired(loaded));
        assertTrue(VultureBodyFeastPolicy.hasEatenBody(loaded, body));

        VultureBodyFeastPolicy.resetRoundState(loaded, 2);

        assertEquals(0, VultureBodyFeastPolicy.bodiesEaten(loaded));
        assertEquals(1, VultureBodyFeastPolicy.bodiesRequired(loaded));
        assertFalse(VultureBodyFeastPolicy.hasEatenBody(loaded, body));
        assertFalse(VultureBodyFeastPolicy.hasWon(loaded));
    }

    @Test
    void resetClearsSavedVultureNeutralWinClaim() {
        NoellesRoleState saved = new NoellesRoleState();
        VultureBodyFeastPolicy.resetRoundState(saved, 2);
        VultureBodyFeastPolicy.recordSuccessfulFeast(saved, UUID.randomUUID(), 180L);

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);
        assertTrue(loaded.neutralWinClaim(VultureBodyFeastPolicy.VULTURE_ROLE).isPresent());

        VultureBodyFeastPolicy.resetRoundState(loaded, 2);

        assertTrue(loaded.neutralWinClaim(VultureBodyFeastPolicy.VULTURE_ROLE).isEmpty());
        assertTrue(NoellesNeutralWinPolicy.contributeRecordedNeutralWins(UUID.randomUUID(), loaded)
                .extraWinners()
                .isEmpty());
    }
}
