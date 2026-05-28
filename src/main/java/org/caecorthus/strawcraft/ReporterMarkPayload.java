package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record ReporterMarkPayload(UUID target) implements CustomPayload {
    public static final Id<ReporterMarkPayload> ID = new Id<>(StrawCraft.id("reporter_mark"));
    public static final PacketCodec<RegistryByteBuf, ReporterMarkPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            ReporterMarkPayload::target,
            ReporterMarkPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
