package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record NoellesNeutralWinResultPayload(UUID winnerUuid, Identifier roleId) implements CustomPayload {
    public static final Id<NoellesNeutralWinResultPayload> ID =
            new Id<>(StrawCraft.id("neutral_win_result"));
    public static final PacketCodec<RegistryByteBuf, NoellesNeutralWinResultPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            NoellesNeutralWinResultPayload::winnerUuid,
            Identifier.PACKET_CODEC,
            NoellesNeutralWinResultPayload::roleId,
            NoellesNeutralWinResultPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
