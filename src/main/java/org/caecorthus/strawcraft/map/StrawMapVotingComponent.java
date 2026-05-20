package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.StrawCraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class StrawMapVotingComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<StrawMapVotingComponent> KEY =
            ComponentRegistry.getOrCreate(StrawCraft.id("map_voting"), StrawMapVotingComponent.class);

    private static final int VOTING_DURATION_TICKS = 30 * 20;
    private static final int ROULETTE_DURATION_TICKS = 8 * 20;
    private static final int ALL_VOTED_REMAINING_TICKS = 5 * 20;

    private final Scoreboard scoreboard;
    @Nullable
    private final MinecraftServer server;

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

    public StrawMapVotingComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public void sync() {
        KEY.sync(scoreboard);
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
        return availableMaps;
    }

    public int[] getVoteCounts() {
        return voteCounts;
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

    public void startVoting(ServerWorld sourceWorld) {
        if (server == null || votingActive || StrawMapRegistry.getInstance().maps().isEmpty()) {
            return;
        }

        GameMode currentMode = GameWorldComponent.KEY.get(sourceWorld).getGameMode();
        Identifier gameModeId = currentMode == null ? WatheGameModes.MURDER_ID : currentMode.identifier;
        int playerCount = server.getPlayerManager().getCurrentPlayerCount();
        List<StrawMapEntry> eligible = StrawMapRegistry.getInstance().eligibleMapsForGameMode(gameModeId, playerCount);
        if (eligible.isEmpty()) {
            return;
        }

        resetForNewVote();
        this.lastSelectedGameMode = gameModeId;
        for (StrawMapEntry entry : eligible) {
            this.availableMaps.add(StrawMapVoteOption.fromEntry(entry));
        }
        this.voteCounts = new int[availableMaps.size()];

        if (availableMaps.size() == 1) {
            this.selectedMapIndex = 0;
            finishSelection();
            return;
        }

        this.votingActive = true;
        this.votingTicksRemaining = VOTING_DURATION_TICKS;
        this.sync();
    }

    public void castVote(UUID playerId, int mapIndex) {
        if (!votingActive || roulettePhase || mapIndex < 0 || mapIndex >= availableMaps.size()) {
            return;
        }

        Integer oldVote = playerVotes.put(playerId, mapIndex);
        if (oldVote != null && oldVote >= 0 && oldVote < voteCounts.length) {
            voteCounts[oldVote] = Math.max(0, voteCounts[oldVote] - 1);
        }
        voteCounts[mapIndex]++;

        if (server != null && playerVotes.size() >= server.getPlayerManager().getCurrentPlayerCount()
                && votingTicksRemaining > ALL_VOTED_REMAINING_TICKS) {
            votingTicksRemaining = ALL_VOTED_REMAINING_TICKS;
        }
        this.sync();
    }

    public boolean skipWaitingPhase() {
        if (!votingActive) {
            return false;
        }
        if (roulettePhase) {
            finishSelection();
        } else {
            endVoting();
        }
        return true;
    }

    @Override
    public void serverTick() {
        if (!votingActive || server == null) {
            return;
        }

        if (roulettePhase) {
            if (--rouletteTicksRemaining <= 0) {
                finishSelection();
            }
            return;
        }

        if (--votingTicksRemaining <= 0) {
            endVoting();
        } else if (votingTicksRemaining % 20 == 0) {
            this.sync();
        }
    }

    private void endVoting() {
        selectedMapIndex = WeightedVotePicker.pick(availableMaps.size(), voteCounts, new Random());
        if (selectedMapIndex < 0) {
            reset();
            return;
        }
        roulettePhase = true;
        rouletteTicksRemaining = ROULETTE_DURATION_TICKS;
        this.sync();
    }

    private void finishSelection() {
        if (server == null || selectedMapIndex < 0 || selectedMapIndex >= availableMaps.size()) {
            reset();
            return;
        }

        StrawMapVoteOption selected = availableMaps.get(selectedMapIndex);
        this.lastSelectedDimension = selected.dimensionId();
        this.lastSelectedGameMode = selected.gameModeId();
        this.votingActive = false;
        this.sync();
        StrawMapVoting.teleportAllPlayersToSelectedMap(server, selected);
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

    public void reset() {
        resetForNewVote();
        this.sync();
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        votingActive = tag.getBoolean("VotingActive");
        votingTicksRemaining = tag.getInt("VotingTicksRemaining");
        roulettePhase = tag.getBoolean("RoulettePhase");
        rouletteTicksRemaining = tag.getInt("RouletteTicksRemaining");
        selectedMapIndex = tag.getInt("SelectedMapIndex");
        lastSelectedDimension = tag.contains("LastSelectedDimension")
                ? Identifier.tryParse(tag.getString("LastSelectedDimension"))
                : null;
        lastSelectedGameMode = tag.contains("LastSelectedGameMode")
                ? Identifier.tryParse(tag.getString("LastSelectedGameMode"))
                : null;

        availableMaps.clear();
        NbtList mapList = tag.getList("AvailableMaps", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : mapList) {
            availableMaps.add(StrawMapVoteOption.fromNbt((NbtCompound) element));
        }
        voteCounts = tag.contains("VoteCounts") ? tag.getIntArray("VoteCounts") : new int[availableMaps.size()];
        playerVotes.clear();
        NbtList votes = tag.getList("PlayerVotes", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : votes) {
            NbtCompound vote = (NbtCompound) element;
            playerVotes.put(vote.getUuid("PlayerId"), vote.getInt("MapIndex"));
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("VotingActive", votingActive);
        tag.putInt("VotingTicksRemaining", votingTicksRemaining);
        tag.putBoolean("RoulettePhase", roulettePhase);
        tag.putInt("RouletteTicksRemaining", rouletteTicksRemaining);
        tag.putInt("SelectedMapIndex", selectedMapIndex);
        if (lastSelectedDimension != null) {
            tag.putString("LastSelectedDimension", lastSelectedDimension.toString());
        }
        if (lastSelectedGameMode != null) {
            tag.putString("LastSelectedGameMode", lastSelectedGameMode.toString());
        }

        NbtList mapList = new NbtList();
        for (StrawMapVoteOption option : availableMaps) {
            mapList.add(option.toNbt());
        }
        tag.put("AvailableMaps", mapList);
        tag.putIntArray("VoteCounts", voteCounts);

        NbtList votes = new NbtList();
        for (Map.Entry<UUID, Integer> entry : playerVotes.entrySet()) {
            NbtCompound vote = new NbtCompound();
            vote.putUuid("PlayerId", entry.getKey());
            vote.putInt("MapIndex", entry.getValue());
            votes.add(vote);
        }
        tag.put("PlayerVotes", votes);
    }
}
