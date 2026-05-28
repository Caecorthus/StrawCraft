package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record SwapperSwapPayload(UUID targetA, UUID targetB) implements CustomPayload {
    public static final Id<SwapperSwapPayload> ID = new Id<>(StrawCraft.id("swapper_swap"));
    public static final PacketCodec<RegistryByteBuf, SwapperSwapPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            SwapperSwapPayload::targetA,
            Uuids.PACKET_CODEC,
            SwapperSwapPayload::targetB,
            SwapperSwapPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
