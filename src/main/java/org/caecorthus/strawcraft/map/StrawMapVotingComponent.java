package org.caecorthus.strawcraft.map;

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

    // The vote belongs to the server scoreboard instead of one world, because the
    // selected result may teleport players across dimensions before the next round starts.
    // 投票状态挂在服务器 scoreboard 上，而不是某个世界上；因为选图结果可能会在下一局开始前把玩家跨维度传送。
    private final Scoreboard scoreboard;
    @Nullable
    private final MinecraftServer server;
    private final MapVotingStateMachine stateMachine = new MapVotingStateMachine();
    private final Random random = new Random();

    public StrawMapVotingComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public void sync() {
        KEY.sync(scoreboard);
    }

    public boolean isVotingActive() {
        return stateMachine.isVotingActive();
    }

    public int getVotingTicksRemaining() {
        return stateMachine.getVotingTicksRemaining();
    }

    public boolean isRoulettePhase() {
        return stateMachine.isRoulettePhase();
    }

    public int getRouletteTicksRemaining() {
        return stateMachine.getRouletteTicksRemaining();
    }

    public int getSelectedMapIndex() {
        return stateMachine.getSelectedMapIndex();
    }

    public List<StrawMapVoteOption> getAvailableMaps() {
        return stateMachine.getAvailableMaps();
    }

    public int[] getVoteCounts() {
        return stateMachine.getVoteCounts();
    }

    public int getVotedMapIndex(UUID playerId) {
        return stateMachine.getVotedMapIndex(playerId);
    }

    @Nullable
    public Identifier getLastSelectedDimension() {
        return stateMachine.getLastSelectedDimension();
    }

    @Nullable
    public Identifier getLastSelectedGameMode() {
        return stateMachine.getLastSelectedGameMode();
    }

    public void startVoting(ServerWorld sourceWorld) {
        if (server == null || stateMachine.isVotingActive() || StrawMapRegistry.getInstance().maps().isEmpty()) {
            return;
        }

        Identifier gameModeId = StrawMapVoting.currentGameModeId(sourceWorld);
        int playerCount = server.getPlayerManager().getCurrentPlayerCount();
        List<StrawMapEntry> eligible = StrawMapRegistry.getInstance().eligibleMapsForGameMode(gameModeId, playerCount);
        if (eligible.isEmpty()) {
            return;
        }

        applyTransition(stateMachine.startVoting(voteOptionsFor(eligible), gameModeId));
    }

    public void castVote(UUID playerId, int mapIndex) {
        // In client-only deserialization contexts there is no server; keep votes valid
        // by making "everyone voted" impossible instead of guessing a player count.
        // 在仅客户端反序列化的上下文里没有 server；这里让“所有人都已投票”不可能触发，而不是猜一个人数。
        int totalPlayerCount = server == null ? Integer.MAX_VALUE : server.getPlayerManager().getCurrentPlayerCount();
        applyTransition(stateMachine.castVote(playerId, mapIndex, totalPlayerCount));
    }

    public boolean skipWaitingPhase() {
        MapVotingStateMachine.Transition transition = stateMachine.skipWaitingPhase(random);
        applyTransition(transition);
        return transition.accepted();
    }

    @Override
    public void serverTick() {
        if (server == null) {
            return;
        }

        applyTransition(stateMachine.tick(random));
    }

    public void reset() {
        applyTransition(stateMachine.reset());
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        Identifier lastSelectedDimension = tag.contains("LastSelectedDimension")
                ? Identifier.tryParse(tag.getString("LastSelectedDimension"))
                : null;
        Identifier lastSelectedGameMode = tag.contains("LastSelectedGameMode")
                ? Identifier.tryParse(tag.getString("LastSelectedGameMode"))
                : null;

        List<StrawMapVoteOption> availableMaps = new ArrayList<>();
        NbtList mapList = tag.getList("AvailableMaps", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : mapList) {
            availableMaps.add(StrawMapVoteOption.fromNbt((NbtCompound) element));
        }
        int[] voteCounts = tag.contains("VoteCounts") ? tag.getIntArray("VoteCounts") : new int[availableMaps.size()];
        Map<UUID, Integer> playerVotes = new HashMap<>();
        NbtList votes = tag.getList("PlayerVotes", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : votes) {
            NbtCompound vote = (NbtCompound) element;
            playerVotes.put(vote.getUuid("PlayerId"), vote.getInt("MapIndex"));
        }

        stateMachine.load(new MapVotingStateMachine.Snapshot(
                tag.getBoolean("VotingActive"),
                tag.getInt("VotingTicksRemaining"),
                tag.getBoolean("RoulettePhase"),
                tag.getInt("RouletteTicksRemaining"),
                tag.getInt("SelectedMapIndex"),
                availableMaps,
                voteCounts,
                playerVotes,
                lastSelectedDimension,
                lastSelectedGameMode
        ));
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        MapVotingStateMachine.Snapshot snapshot = stateMachine.snapshot();
        tag.putBoolean("VotingActive", snapshot.votingActive());
        tag.putInt("VotingTicksRemaining", snapshot.votingTicksRemaining());
        tag.putBoolean("RoulettePhase", snapshot.roulettePhase());
        tag.putInt("RouletteTicksRemaining", snapshot.rouletteTicksRemaining());
        tag.putInt("SelectedMapIndex", snapshot.selectedMapIndex());
        if (snapshot.lastSelectedDimension() != null) {
            tag.putString("LastSelectedDimension", snapshot.lastSelectedDimension().toString());
        }
        if (snapshot.lastSelectedGameMode() != null) {
            tag.putString("LastSelectedGameMode", snapshot.lastSelectedGameMode().toString());
        }

        NbtList mapList = new NbtList();
        for (StrawMapVoteOption option : snapshot.availableMaps()) {
            mapList.add(option.toNbt());
        }
        tag.put("AvailableMaps", mapList);
        tag.putIntArray("VoteCounts", snapshot.voteCounts());

        NbtList votes = new NbtList();
        for (Map.Entry<UUID, Integer> entry : snapshot.playerVotes().entrySet()) {
            NbtCompound vote = new NbtCompound();
            vote.putUuid("PlayerId", entry.getKey());
            vote.putInt("MapIndex", entry.getValue());
            votes.add(vote);
        }
        tag.put("PlayerVotes", votes);
    }

    private static List<StrawMapVoteOption> voteOptionsFor(List<StrawMapEntry> entries) {
        List<StrawMapVoteOption> options = new ArrayList<>(entries.size());
        for (StrawMapEntry entry : entries) {
            options.add(StrawMapVoteOption.fromEntry(entry));
        }
        return options;
    }

    private void applyTransition(MapVotingStateMachine.Transition transition) {
        if (transition.sync()) {
            this.sync();
        }
        if (server != null) {
            StrawMapVoting.applyFinishEffects(server, transition);
        }
    }
}
