package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.MapEffect;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheMapEffects;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.caecorthus.strawcraft.StrawCraft;

class MapVotingEffectApplier {
    void applyFinishEffects(MinecraftServer server, MapVoteFinishPlan plan) {
        ServerWorld targetWorld = worldFor(server, plan.dimensionId());
        if (targetWorld == null) {
            StrawCraft.LOGGER.warn("Cannot teleport map voters: target dimension {} is not loaded", plan.dimensionId());
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(targetWorld);
        game.setGameMode(gameMode(plan.gameModeId()));
        game.setMapEffect(mapEffect(plan.mapEffectId()));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            teleportToMapSpawn(player, targetWorld);
        }
    }

    private static ServerWorld worldFor(MinecraftServer server, Identifier dimensionId) {
        return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
    }

    private static GameMode gameMode(Identifier id) {
        return WatheGameModes.GAME_MODES.getOrDefault(id, WatheGameModes.MURDER);
    }

    private static MapEffect mapEffect(Identifier id) {
        return WatheMapEffects.MAP_EFFECTS.getOrDefault(id, WatheMapEffects.GENERIC);
    }

    private static void teleportToMapSpawn(ServerPlayerEntity player, ServerWorld targetWorld) {
        MapVariablesWorldComponent.PosWithOrientation spawn = MapVariablesWorldComponent.KEY.get(targetWorld).getSpawnPos();
        player.teleportTo(new TeleportTarget(
                targetWorld,
                spawn.pos,
                Vec3d.ZERO,
                spawn.yaw,
                spawn.pitch,
                TeleportTarget.NO_OP
        ));
    }
}
