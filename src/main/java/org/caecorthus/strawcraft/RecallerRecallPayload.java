package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RecallerRecallPayload() implements CustomPayload {
    public static final Id<RecallerRecallPayload> ID = new Id<>(StrawCraft.id("recaller_recall"));
    public static final PacketCodec<RegistryByteBuf, RecallerRecallPayload> CODEC =
            PacketCodec.unit(new RecallerRecallPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
