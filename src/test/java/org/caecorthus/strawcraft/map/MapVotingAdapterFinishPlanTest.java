package org.caecorthus.strawcraft.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapVotingAdapterFinishPlanTest {
    private static final Identifier GAME_MODE = Identifier.of("wathe", "murder");

    @Test
    void mapVotingInvokesEffectApplierOnceWithFinishPlan() {
        StrawMapVoteOption station = option("station");
        MapVotingStateMachine.Transition finished = new MapVotingStateMachine.Transition(
                true,
                true,
                Optional.of(station)
        );
        RecordingEffectApplier applier = new RecordingEffectApplier();

        StrawMapVoting.applyFinishEffects(null, finished, applier);

        assertEquals(1, applier.calls);
        assertEquals(MapVoteFinishPlan.from(station), applier.plan);
    }

    @Test
    void mapVotingDoesNotInvokeEffectApplierWithoutFinishPlan() {
        RecordingEffectApplier applier = new RecordingEffectApplier();

        StrawMapVoting.applyFinishEffects(null, new MapVotingStateMachine.Transition(
                true,
                true,
                Optional.empty()
        ), applier);

        assertEquals(0, applier.calls);
        assertEquals(null, applier.plan);
    }

    @Test
    void adapterCreatesFinishPlanForSelectedMapWhenVoteFinishes() {
        MapVotingStateMachine voting = new MapVotingStateMachine();
        StrawMapVoteOption mansion = option("mansion");
        StrawMapVoteOption station = option("station");
        voting.startVoting(List.of(mansion, station), GAME_MODE);
        voting.castVote(UUID.randomUUID(), 1, 1);
        voting.skipWaitingPhase(fixedRoll(0));

        MapVotingStateMachine.Transition finished = voting.skipWaitingPhase(fixedRoll(0));

        MapVoteFinishPlan plan = StrawMapVoting.finishPlan(finished).orElseThrow();
        assertEquals(station.dimensionId(), plan.dimensionId());
        assertEquals(station.gameModeId(), plan.gameModeId());
        assertEquals(station.mapEffectId(), plan.mapEffectId());
    }

    @Test
    void adapterDoesNotCreateFinishPlanBeforeSelectedMapIsReady() {
        MapVotingStateMachine voting = new MapVotingStateMachine();
        voting.startVoting(List.of(option("mansion"), option("station")), GAME_MODE);
        voting.castVote(UUID.randomUUID(), 1, 1);

        MapVotingStateMachine.Transition roulette = voting.skipWaitingPhase(fixedRoll(0));

        assertEquals(Optional.empty(), StrawMapVoting.finishPlan(roulette));
    }

    private static StrawMapVoteOption option(String path) {
        Identifier id = Identifier.of("strawcraft", path);
        return new StrawMapVoteOption(
                id,
                Identifier.of("strawcraft", path + "_dimension"),
                GAME_MODE,
                Identifier.of("wathe", path + "_effect"),
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

    private static final class RecordingEffectApplier extends MapVotingEffectApplier {
        private int calls;
        private MapVoteFinishPlan plan;

        @Override
        void applyFinishEffects(MinecraftServer ignoredServer, MapVoteFinishPlan plan) {
            calls++;
            this.plan = plan;
        }
    }
}
