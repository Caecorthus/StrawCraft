package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record PhantomInvisibilityPayload() implements CustomPayload {
    public static final Id<PhantomInvisibilityPayload> ID = new Id<>(StrawCraft.id("phantom_invisibility"));
    public static final PacketCodec<RegistryByteBuf, PhantomInvisibilityPayload> CODEC =
            PacketCodec.unit(new PhantomInvisibilityPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
