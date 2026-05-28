package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathogenWinPolicyTest {
    private static final UUID PATHOGEN = new UUID(0, 1);
    private static final UUID OTHER_PATHOGEN = new UUID(0, 2);
    private static final UUID INFECTED = new UUID(0, 3);
    private static final UUID UNINFECTED = new UUID(0, 4);

    @Test
    void allAliveAssignedNonPathogensInfectedBySamePathogenRecordsNeutralClaim() {
        NoellesRoleState state = new NoellesRoleState();

        boolean claimed = PathogenWinPolicy.recordNeutralWinIfComplete(
                state,
                PATHOGEN,
                List.of(
                        participant(PATHOGEN, true, true, true, Optional.empty()),
                        participant(INFECTED, true, true, false, Optional.of(PATHOGEN)),
                        participant(OTHER_PATHOGEN, true, true, true, Optional.empty())
                ),
                500L
        );

        assertTrue(claimed);
        NoellesRoleState.NeutralWinClaim claim = state.neutralWinClaim(PathogenWinPolicy.PATHOGEN_ROLE).orElseThrow();
        assertEquals(PathogenWinPolicy.INFECTION_WIN_TRIGGER, claim.trigger());
        assertEquals(500L, claim.gameTime());
    }

    @Test
    void deadAndUnassignedPlayersDoNotBlockPathogenClaim() {
        NoellesRoleState state = new NoellesRoleState();

        assertTrue(PathogenWinPolicy.recordNeutralWinIfComplete(
                state,
                PATHOGEN,
                List.of(
                        participant(PATHOGEN, true, true, true, Optional.empty()),
                        participant(INFECTED, true, true, false, Optional.of(PATHOGEN)),
                        participant(UNINFECTED, true, false, false, Optional.empty()),
                        participant(UUID.randomUUID(), false, true, false, Optional.empty())
                ),
                600L
        ));
    }

    @Test
    void partialInfectionDoesNotClaim() {
        NoellesRoleState state = new NoellesRoleState();

        assertFalse(PathogenWinPolicy.recordNeutralWinIfComplete(
                state,
                PATHOGEN,
                List.of(
                        participant(PATHOGEN, true, true, true, Optional.empty()),
                        participant(INFECTED, true, true, false, Optional.of(PATHOGEN)),
                        participant(UNINFECTED, true, true, false, Optional.empty())
                ),
                700L
        ));
        assertTrue(state.neutralWinClaims().isEmpty());
    }

    @Test
    void multiPathogenClaimsAreOwnedByThePathogenThatInfectedEveryLiveNonPathogen() {
        NoellesRoleState firstState = new NoellesRoleState();
        NoellesRoleState secondState = new NoellesRoleState();

        List<PathogenWinPolicy.Participant> participants = List.of(
                participant(PATHOGEN, true, true, true, Optional.empty()),
                participant(OTHER_PATHOGEN, true, true, true, Optional.empty()),
                participant(INFECTED, true, true, false, Optional.of(OTHER_PATHOGEN))
        );

        assertFalse(PathogenWinPolicy.recordNeutralWinIfComplete(firstState, PATHOGEN, participants, 800L));
        assertTrue(PathogenWinPolicy.recordNeutralWinIfComplete(secondState, OTHER_PATHOGEN, participants, 800L));
        assertTrue(firstState.neutralWinClaims().isEmpty());
        assertTrue(secondState.neutralWinClaim(PathogenWinPolicy.PATHOGEN_ROLE).isPresent());
    }

    private static PathogenWinPolicy.Participant participant(
            UUID uuid,
            boolean assigned,
            boolean alive,
            boolean pathogen,
            Optional<UUID> infectedBy
    ) {
        return new PathogenWinPolicy.Participant(uuid, assigned, alive, pathogen, infectedBy);
    }
}
