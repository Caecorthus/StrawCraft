package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record DetectiveInvestigationPayload(UUID targetPlayer) implements CustomPayload {
    public static final Id<DetectiveInvestigationPayload> ID = new Id<>(StrawCraft.id("detective_investigate"));
    public static final PacketCodec<RegistryByteBuf, DetectiveInvestigationPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            DetectiveInvestigationPayload::targetPlayer,
            DetectiveInvestigationPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
