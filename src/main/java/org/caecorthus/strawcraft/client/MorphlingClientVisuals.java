package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.MorphlingDisguisePolicy;
import org.caecorthus.strawcraft.NoellesRoleState;
import org.caecorthus.strawcraft.NoellesRoleStateComponent;

import java.util.Optional;
import java.util.UUID;

public final class MorphlingClientVisuals {
    private MorphlingClientVisuals() {
    }

    public static boolean isCorpseMode(PlayerEntity player) {
        return NoellesRoleStateComponent.KEY.get(player).morphlingDisguiseState().corpseMode();
    }

    public static Optional<UUID> activeDisguiseUuid(PlayerEntity player) {
        NoellesRoleState.MorphlingDisguiseState state =
                NoellesRoleStateComponent.KEY.get(player).morphlingDisguiseState();
        if (!MorphlingDisguisePolicy.isActive(state)) {
            return Optional.empty();
        }
        return state.disguiseUuid();
    }

    public static Optional<SkinTextures> disguiseSkinTextures(AbstractClientPlayerEntity player) {
        return activeDisguiseUuid(player).flatMap(uuid -> skinTexturesFor(player, uuid));
    }

    public static Optional<SkinTextures.Model> disguiseModel(AbstractClientPlayerEntity player) {
        return disguiseSkinTextures(player).map(SkinTextures::model);
    }

    public static Optional<Text> disguiseDisplayName(PlayerEntity player) {
        return activeDisguiseUuid(player).flatMap(uuid -> displayNameFor(player, uuid));
    }

    private static Optional<SkinTextures> skinTexturesFor(AbstractClientPlayerEntity player, UUID disguiseUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && disguiseUuid.equals(client.player.getUuid())) {
            return Optional.of(client.player.getSkinTextures());
        }
        PlayerEntity target = player.getWorld().getPlayerByUuid(disguiseUuid);
        if (target instanceof AbstractClientPlayerEntity clientTarget) {
            return Optional.of(clientTarget.getSkinTextures());
        }
        PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
        return cachedEntry == null ? Optional.empty() : Optional.of(cachedEntry.getSkinTextures());
    }

    private static Optional<Text> displayNameFor(PlayerEntity player, UUID disguiseUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && disguiseUuid.equals(client.player.getUuid())) {
            return Optional.of(client.player.getDisplayName());
        }
        PlayerEntity target = player.getWorld().getPlayerByUuid(disguiseUuid);
        if (target != null) {
            return Optional.of(target.getDisplayName());
        }
        PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid);
        if (cachedEntry == null) {
            return Optional.empty();
        }
        Text displayName = cachedEntry.getDisplayName();
        return Optional.of(displayName == null ? Text.literal(cachedEntry.getProfile().getName()) : displayName);
    }
}
