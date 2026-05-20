package org.caecorthus.strawcraft.map;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class MapVotingStateMachine {
    public static final int VOTING_DURATION_TICKS = 30 * 20;
    public static final int ROULETTE_DURATION_TICKS = 8 * 20;
    public static final int ALL_VOTED_REMAINING_TICKS = 5 * 20;

    private boolean votingActive;
    private int votingTicksRemaining;
    private boolean roulettePhase;
    private int rouletteTicksRemaining;
    private int selectedMapIndex = -1;
    private final List<StrawMapVoteOption> availableMaps = new ArrayList<>();
    private int[] voteCounts = new int[0];
    private final Map<UUID, Integer> playerVotes = new HashMap<>();
    @Nullable
    private Identifier lastSelectedDimension;
    @Nullable
    private Identifier lastSelectedGameMode;

    public Transition startVoting(List<StrawMapVoteOption> eligibleMaps, Identifier gameModeId) {
        if (votingActive || eligibleMaps.isEmpty()) {
            return Transition.noop();
        }

        resetForNewVote();
        this.lastSelectedGameMode = gameModeId;
        this.availableMaps.addAll(eligibleMaps);
        this.voteCounts = new int[availableMaps.size()];

        if (availableMaps.size() == 1) {
            this.selectedMapIndex = 0;
            return finishSelection();
        }

        this.votingActive = true;
        this.votingTicksRemaining = VOTING_DURATION_TICKS;
        return Transition.synced();
    }

    public Transition castVote(UUID playerId, int mapIndex, int totalPlayerCount) {
        if (!votingActive || roulettePhase || mapIndex < 0 || mapIndex >= availableMaps.size()) {
            return Transition.noop();
        }

        Integer oldVote = playerVotes.put(playerId, mapIndex);
        if (oldVote != null && oldVote >= 0 && oldVote < voteCounts.length) {
            voteCounts[oldVote] = Math.max(0, voteCounts[oldVote] - 1);
        }
        voteCounts[mapIndex]++;

        if (playerVotes.size() >= totalPlayerCount && votingTicksRemaining > ALL_VOTED_REMAINING_TICKS) {
            votingTicksRemaining = ALL_VOTED_REMAINING_TICKS;
        }
        return Transition.synced();
    }

    public Transition skipWaitingPhase(RandomGenerator random) {
        if (!votingActive) {
            return Transition.noop();
        }
        if (roulettePhase) {
            return finishSelection();
        }
        return endVoting(random);
    }

    public Transition tick(RandomGenerator random) {
        if (!votingActive) {
            return Transition.noop();
        }

        if (roulettePhase) {
            if (--rouletteTicksRemaining <= 0) {
                return finishSelection();
            }
            return Transition.noop();
        }

        if (--votingTicksRemaining <= 0) {
            return endVoting(random);
        }
        if (votingTicksRemaining % 20 == 0) {
            return Transition.synced();
        }
        return Transition.noop();
    }

    public Transition reset() {
        resetForNewVote();
        return Transition.synced();
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public int getVotingTicksRemaining() {
        return votingTicksRemaining;
    }

    public boolean isRoulettePhase() {
        return roulettePhase;
    }

    public int getRouletteTicksRemaining() {
        return rouletteTicksRemaining;
    }

    public int getSelectedMapIndex() {
        return selectedMapIndex;
    }

    public List<StrawMapVoteOption> getAvailableMaps() {
        return List.copyOf(availableMaps);
    }

    public int[] getVoteCounts() {
        return Arrays.copyOf(voteCounts, voteCounts.length);
    }

    public int getVotedMapIndex(UUID playerId) {
        return playerVotes.getOrDefault(playerId, -1);
    }

    @Nullable
    public Identifier getLastSelectedDimension() {
        return lastSelectedDimension;
    }

    @Nullable
    public Identifier getLastSelectedGameMode() {
        return lastSelectedGameMode;
    }

    public Snapshot snapshot() {
        return new Snapshot(
                votingActive,
                votingTicksRemaining,
                roulettePhase,
                rouletteTicksRemaining,
                selectedMapIndex,
                List.copyOf(availableMaps),
                Arrays.copyOf(voteCounts, voteCounts.length),
                Map.copyOf(playerVotes),
                lastSelectedDimension,
                lastSelectedGameMode
        );
    }

    public void load(Snapshot snapshot) {
        this.votingActive = snapshot.votingActive();
        this.votingTicksRemaining = snapshot.votingTicksRemaining();
        this.roulettePhase = snapshot.roulettePhase();
        this.rouletteTicksRemaining = snapshot.rouletteTicksRemaining();
        this.selectedMapIndex = snapshot.selectedMapIndex();
        this.availableMaps.clear();
        this.availableMaps.addAll(snapshot.availableMaps());
        this.voteCounts = Arrays.copyOf(snapshot.voteCounts(), snapshot.voteCounts().length);
        this.playerVotes.clear();
        this.playerVotes.putAll(snapshot.playerVotes());
        this.lastSelectedDimension = snapshot.lastSelectedDimension();
        this.lastSelectedGameMode = snapshot.lastSelectedGameMode();
    }

    private Transition endVoting(RandomGenerator random) {
        selectedMapIndex = WeightedVotePicker.pick(availableMaps.size(), voteCounts, random);
        if (selectedMapIndex < 0) {
            return reset();
        }
        roulettePhase = true;
        rouletteTicksRemaining = ROULETTE_DURATION_TICKS;
        return Transition.synced();
    }

    private Transition finishSelection() {
        if (selectedMapIndex < 0 || selectedMapIndex >= availableMaps.size()) {
            return reset();
        }

        StrawMapVoteOption selected = availableMaps.get(selectedMapIndex);
        this.lastSelectedDimension = selected.dimensionId();
        this.lastSelectedGameMode = selected.gameModeId();
        this.votingActive = false;
        return Transition.finish(selected);
    }

    private void resetForNewVote() {
        votingActive = false;
        votingTicksRemaining = 0;
        roulettePhase = false;
        rouletteTicksRemaining = 0;
        selectedMapIndex = -1;
        availableMaps.clear();
        voteCounts = new int[0];
        playerVotes.clear();
    }

    public record Transition(boolean accepted, boolean sync, Optional<StrawMapVoteOption> selectedMap) {
        private static Transition noop() {
            return new Transition(false, false, Optional.empty());
        }

        private static Transition synced() {
            return new Transition(true, true, Optional.empty());
        }

        private static Transition finish(StrawMapVoteOption selectedMap) {
            return new Transition(true, true, Optional.of(selectedMap));
        }
    }

    public record Snapshot(
            boolean votingActive,
            int votingTicksRemaining,
            boolean roulettePhase,
            int rouletteTicksRemaining,
            int selectedMapIndex,
            List<StrawMapVoteOption> availableMaps,
            int[] voteCounts,
            Map<UUID, Integer> playerVotes,
            @Nullable Identifier lastSelectedDimension,
            @Nullable Identifier lastSelectedGameMode
    ) {
    }
}
