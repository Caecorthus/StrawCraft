package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record MorphlingCorpseTogglePayload() implements CustomPayload {
    public static final Id<MorphlingCorpseTogglePayload> ID = new Id<>(StrawCraft.id("morph_corpse_toggle"));
    public static final PacketCodec<RegistryByteBuf, MorphlingCorpseTogglePayload> CODEC =
            PacketCodec.unit(new MorphlingCorpseTogglePayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
