package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record VultureFeastPayload() implements CustomPayload {
    public static final Id<VultureFeastPayload> ID = new Id<>(StrawCraft.id("vulture_feast"));
    public static final PacketCodec<RegistryByteBuf, VultureFeastPayload> CODEC =
            PacketCodec.unit(new VultureFeastPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
