package org.caecorthus.strawcraft;

import org.caecorthus.strawcraft.api.StrawWinEvents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaotieWinContributionTest {
    @Test
    void allRemainingEligibleOpponentsSwallowedReplacesDefaultWithLooseEndClaim() {
        UUID taotie = UUID.randomUUID();
        UUID swallowedOpponent = UUID.randomUUID();
        UUID deadOutside = UUID.randomUUID();
        UUID unassignedSpectator = UUID.randomUUID();
        StrawWinEvents.WinContribution.Builder builder = StrawWinEvents.WinContribution.builder();

        TaotieSwallowRuntime.collectWinContribution(
                taotie,
                Set.of(swallowedOpponent),
                List.of(
                        participant(taotie, true, true),
                        participant(swallowedOpponent, true, false),
                        participant(deadOutside, true, false),
                        participant(unassignedSpectator, false, false)
                ),
                builder
        );

        StrawWinEvents.WinContribution contribution = builder.build();
        assertFalse(contribution.suppressDefaultWin());
        assertEquals(Optional.of(StrawWinEvents.DefaultWin.LOOSE_END), contribution.replacementDefaultWin());
        assertTrue(contribution.extraWinners().contains(new StrawWinEvents.ExtraWinner(
                taotie,
                TaotieSwallowPolicy.TAOTIE_ROLE,
                TaotieSwallowRuntime.SWALLOWED_ALL_TRIGGER
        )));
    }

    @Test
    void partialSwallowLeavesDefaultWinUntouched() {
        UUID taotie = UUID.randomUUID();
        UUID swallowedOpponent = UUID.randomUUID();
        UUID liveOpponent = UUID.randomUUID();
        StrawWinEvents.WinContribution.Builder builder = StrawWinEvents.WinContribution.builder();

        TaotieSwallowRuntime.collectWinContribution(
                taotie,
                Set.of(swallowedOpponent),
                List.of(
                        participant(taotie, true, true),
                        participant(swallowedOpponent, true, false),
                        participant(liveOpponent, true, true)
                ),
                builder
        );

        StrawWinEvents.WinContribution contribution = builder.build();
        assertFalse(contribution.suppressDefaultWin());
        assertTrue(contribution.replacementDefaultWin().isEmpty());
        assertTrue(contribution.extraWinners().isEmpty());
    }

    private static StrawWinEvents.Participant participant(UUID playerUuid, boolean assigned, boolean alive) {
        return new StrawWinEvents.Participant(
                playerUuid,
                assigned,
                alive,
                assigned ? Optional.of(WatheRoleIds.CIVILIAN) : Optional.empty()
        );
    }
}
