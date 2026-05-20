package org.caecorthus.strawcraft.map;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapVotingStateMachineTest {
    private static final Identifier GAME_MODE = Identifier.of("wathe", "murder");

    @Test
    void allOnlinePlayersVotingShortensTheRemainingVotingTime() {
        MapVotingStateMachine voting = new MapVotingStateMachine();
        voting.startVoting(List.of(option("mansion"), option("station")), GAME_MODE);

        voting.castVote(UUID.randomUUID(), 0, 2);
        assertEquals(MapVotingStateMachine.VOTING_DURATION_TICKS, voting.getVotingTicksRemaining());

        voting.castVote(UUID.randomUUID(), 1, 2);

        assertTrue(voting.isVotingActive());
        assertFalse(voting.isRoulettePhase());
        assertEquals(MapVotingStateMachine.ALL_VOTED_REMAINING_TICKS, voting.getVotingTicksRemaining());
        assertEquals(1, voting.getVoteCounts()[0]);
        assertEquals(1, voting.getVoteCounts()[1]);
    }

    @Test
    void skipMovesVotingToRouletteThenFinishesTheSelectedMap() {
        MapVotingStateMachine voting = new MapVotingStateMachine();
        StrawMapVoteOption mansion = option("mansion");
        StrawMapVoteOption station = option("station");
        voting.startVoting(List.of(mansion, station), GAME_MODE);
        voting.castVote(UUID.randomUUID(), 1, 1);

        MapVotingStateMachine.Transition roulette = voting.skipWaitingPhase(fixedRoll(0));

        assertTrue(roulette.accepted());
        assertTrue(roulette.sync());
        assertTrue(roulette.selectedMap().isEmpty());
        assertTrue(voting.isRoulettePhase());
        assertEquals(1, voting.getSelectedMapIndex());

        MapVotingStateMachine.Transition finished = voting.skipWaitingPhase(fixedRoll(0));

        assertTrue(finished.accepted());
        assertTrue(finished.sync());
        assertEquals(Optional.of(station), finished.selectedMap());
        assertFalse(voting.isVotingActive());
        assertEquals(station.dimensionId(), voting.getLastSelectedDimension());
        assertEquals(station.gameModeId(), voting.getLastSelectedGameMode());
    }

    @Test
    void singleEligibleMapFinishesImmediately() {
        MapVotingStateMachine voting = new MapVotingStateMachine();
        StrawMapVoteOption mansion = option("mansion");

        MapVotingStateMachine.Transition transition = voting.startVoting(List.of(mansion), GAME_MODE);

        assertTrue(transition.accepted());
        assertTrue(transition.sync());
        assertEquals(Optional.of(mansion), transition.selectedMap());
        assertFalse(voting.isVotingActive());
        assertEquals(0, voting.getSelectedMapIndex());
    }

    private static StrawMapVoteOption option(String path) {
        Identifier id = Identifier.of("strawcraft", path);
        return new StrawMapVoteOption(
                id,
                Identifier.of("strawcraft", path + "_dimension"),
                GAME_MODE,
                Identifier.of("wathe", "generic"),
                path,
                "",
                2,
                100
        );
    }

    private static RandomGenerator fixedRoll(int value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return Math.floorMod(value, bound);
            }
        };
    }
}
