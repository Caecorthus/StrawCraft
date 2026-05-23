package org.caecorthus.strawcraft.map;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Optional;

final class StrawCurrentMapResolver {
    private StrawCurrentMapResolver() {
    }

    static Optional<StrawMapEntry> resolve(World world, GameWorldComponent gameComponent) {
        return resolve(world.getRegistryKey().getValue(), gameComponent);
    }

    static Optional<StrawMapEntry> resolve(Identifier dimensionId, GameWorldComponent gameComponent) {
        return resolve(dimensionId, gameModeId(gameComponent), mapEffectId(gameComponent));
    }

    static Optional<StrawMapEntry> resolve(Identifier dimensionId, Identifier gameModeId, Identifier mapEffectId) {
        return StrawMapRegistry.getInstance().mapFor(dimensionId, gameModeId, mapEffectId);
    }

    private static Identifier gameModeId(GameWorldComponent gameComponent) {
        return gameComponent.getGameMode() == null
                ? StrawMapEntry.DEFAULT_GAME_MODE
                : gameComponent.getGameMode().identifier;
    }

    private static Identifier mapEffectId(GameWorldComponent gameComponent) {
        return gameComponent.getMapEffect() == null
                ? null
                : gameComponent.getMapEffect().identifier;
    }
}
