package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record MorphlingDisguisePayload(UUID target) implements CustomPayload {
    public static final Id<MorphlingDisguisePayload> ID = new Id<>(StrawCraft.id("morph"));
    public static final PacketCodec<RegistryByteBuf, MorphlingDisguisePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            MorphlingDisguisePayload::target,
            MorphlingDisguisePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
