package org.caecorthus.strawcraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record AssassinGuessPayload(UUID targetUuid, Identifier guessedRoleId) implements CustomPayload {
    public static final Id<AssassinGuessPayload> ID = new Id<>(StrawCraft.id("assassin_guess"));
    public static final PacketCodec<RegistryByteBuf, AssassinGuessPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            AssassinGuessPayload::targetUuid,
            Identifier.PACKET_CODEC,
            AssassinGuessPayload::guessedRoleId,
            AssassinGuessPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
