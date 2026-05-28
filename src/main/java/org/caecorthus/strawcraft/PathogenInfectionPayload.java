package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record PathogenInfectionPayload() implements CustomPayload {
    public static final Id<PathogenInfectionPayload> ID = new Id<>(StrawCraft.id("pathogen_infection"));
    public static final PacketCodec<RegistryByteBuf, PathogenInfectionPayload> CODEC =
            PacketCodec.unit(new PathogenInfectionPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
