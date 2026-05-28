package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record VoodooBondPayload(UUID target) implements CustomPayload {
    public static final Id<VoodooBondPayload> ID = new Id<>(StrawCraft.id("voodoo_bond"));
    public static final PacketCodec<RegistryByteBuf, VoodooBondPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            VoodooBondPayload::target,
            VoodooBondPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
