package org.caecorthus.strawcraft.map;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapVotingArchitectureTest {
    private static final Path MAP_SOURCE_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft/map");
    private static final Path COMPONENT_SOURCE = MAP_SOURCE_ROOT.resolve("StrawMapVotingComponent.java");
    private static final Set<String> SELECTED_MAP_APPLICATION_SIDE_EFFECT_FILES = Set.of(
            "MapVotingEffectApplier.java",
            "StrawMapVoting.java"
    );
    private static final Set<String> MAP_ENHANCEMENT_SIDE_EFFECT_FILES = Set.of(
            "StrawInteractionBlacklistAdapter.java",
            "StrawPlayerEnhancementAdapter.java",
            "StrawRoomEnhancementAdapter.java"
    );

    @Test
    void componentDoesNotApplySelectedMapSideEffectsDirectly() throws IOException {
        String component = read(COMPONENT_SOURCE);

        assertFalse(component.contains("StrawMapVoting.teleportAllPlayersToSelectedMap"),
                "StrawMapVotingComponent should persist/tick/sync voting state, not trigger map application");
        assertFalse(component.contains("GameWorldComponent.KEY"),
                "StrawMapVotingComponent should not reach into Wathe game components to resolve/apply modes");
        assertFalse(component.contains("MapVariablesWorldComponent"),
                "StrawMapVotingComponent should not read or write Wathe map variables directly");
        assertFalse(component.contains("setGameMode("),
                "StrawMapVotingComponent should not write the selected Wathe game mode directly");
        assertFalse(component.contains("setMapEffect("),
                "StrawMapVotingComponent should not write the selected Wathe map effect directly");
    }

    @Test
    void selectedMapApplicationSideEffectsStayInMapVotingAdapterCode() throws IOException {
        List<Path> filesWithSideEffects;
        try (var paths = Files.walk(MAP_SOURCE_ROOT)) {
            filesWithSideEffects = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(MapVotingArchitectureTest::containsMapApplySideEffect)
                    .toList();
        }

        assertTrue(filesWithSideEffects.stream().allMatch(MapVotingArchitectureTest::isAllowedMapSideEffectFile),
                "Wathe/Fabric map application side effects should stay in the map voting applier, "
                        + "or in explicitly reviewed map enhancement adapters for unrelated runtime effects: "
                        + filesWithSideEffects);
    }

    private static boolean containsMapApplySideEffect(Path path) {
        try {
            String source = read(path);
            return source.contains("teleportAllPlayersToSelectedMap")
                    || source.contains("GameWorldComponent.KEY")
                    || source.contains("MapVariablesWorldComponent.KEY")
                    || source.contains("setGameMode(")
                    || source.contains("setMapEffect(")
                    || source.contains("teleportTo(");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }

    private static boolean isAllowedMapSideEffectFile(Path path) {
        String fileName = path.getFileName().toString();
        return SELECTED_MAP_APPLICATION_SIDE_EFFECT_FILES.contains(fileName)
                || MAP_ENHANCEMENT_SIDE_EFFECT_FILES.contains(fileName);
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
