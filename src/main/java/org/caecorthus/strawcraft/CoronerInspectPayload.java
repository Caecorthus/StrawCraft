package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record CoronerInspectPayload() implements CustomPayload {
    public static final Id<CoronerInspectPayload> ID = new Id<>(StrawCraft.id("coroner_inspect"));
    public static final PacketCodec<RegistryByteBuf, CoronerInspectPayload> CODEC =
            PacketCodec.unit(new CoronerInspectPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
