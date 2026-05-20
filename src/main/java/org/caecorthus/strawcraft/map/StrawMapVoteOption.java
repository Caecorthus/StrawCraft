package org.caecorthus.strawcraft.map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public record StrawMapVoteOption(
        Identifier mapId,
        Identifier dimensionId,
        Identifier gameModeId,
        Identifier mapEffectId,
        String displayName,
        String description,
        int minPlayers,
        int maxPlayers
) {
    public static StrawMapVoteOption fromEntry(StrawMapEntry entry) {
        return new StrawMapVoteOption(
                entry.id(),
                entry.dimensionId(),
                entry.gameModeId(),
                entry.mapEffectId(),
                entry.displayName(),
                entry.description().orElse(""),
                entry.minPlayers(),
                entry.maxPlayers()
        );
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("MapId", mapId.toString());
        tag.putString("DimensionId", dimensionId.toString());
        tag.putString("GameModeId", gameModeId.toString());
        tag.putString("MapEffectId", mapEffectId.toString());
        tag.putString("DisplayName", displayName);
        tag.putString("Description", description);
        tag.putInt("MinPlayers", minPlayers);
        tag.putInt("MaxPlayers", maxPlayers);
        return tag;
    }

    public static StrawMapVoteOption fromNbt(NbtCompound tag) {
        return new StrawMapVoteOption(
                Identifier.tryParse(tag.getString("MapId")),
                Identifier.tryParse(tag.getString("DimensionId")),
                Identifier.tryParse(tag.getString("GameModeId")),
                Identifier.tryParse(tag.getString("MapEffectId")),
                tag.getString("DisplayName"),
                tag.getString("Description"),
                tag.getInt("MinPlayers"),
                tag.getInt("MaxPlayers")
        );
    }
}
