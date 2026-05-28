package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record TaotieSwallowPayload(UUID target) implements CustomPayload {
    public static final Id<TaotieSwallowPayload> ID = new Id<>(StrawCraft.id("taotie_swallow"));
    public static final PacketCodec<RegistryByteBuf, TaotieSwallowPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            TaotieSwallowPayload::target,
            TaotieSwallowPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
