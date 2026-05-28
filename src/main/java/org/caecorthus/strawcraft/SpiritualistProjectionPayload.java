package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SpiritualistProjectionPayload() implements CustomPayload {
    public static final Id<SpiritualistProjectionPayload> ID = new Id<>(StrawCraft.id("spiritualist_projection"));
    public static final PacketCodec<RegistryByteBuf, SpiritualistProjectionPayload> CODEC =
            PacketCodec.unit(new SpiritualistProjectionPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
