package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsaneParanoidKillerFoundationArchitectureTest {
    private static final Path MAIN_ROOT = Path.of("src/main/java/org/caecorthus/strawcraft");
    private static final Path POLICY = MAIN_ROOT.resolve("InsaneParanoidKillerPolicy.java");
    private static final Path CATALOG = MAIN_ROOT.resolve("NoellesRoleCatalog.java");
    private static final Path INITIALIZER = MAIN_ROOT.resolve("StrawCraft.java");
    private static final Path CLIENT = MAIN_ROOT.resolve("client/StrawCraftClient.java");
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/strawcraft.mixins.json");
    private static final Path FABRIC_MOD = Path.of("src/main/resources/fabric.mod.json");

    @Test
    void policyModelsOnlyTheEvidenceBackedPredicates() throws IOException {
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);

        assertTrue(policy.contains("ROLE_ID"));
        assertTrue(policy.contains("shouldForwardDeadVoiceToInsaneKiller"));
        assertTrue(policy.contains("shouldRenderHallucinatedPlayer"));
        assertTrue(policy.contains("deadOrSpectator()"));
        assertTrue(policy.contains("!sender.swallowed()"));
        assertTrue(policy.contains("target.aliveAndPlaying()"));
        assertTrue(policy.contains("target.distanceToSender() <= voiceDistance"));
        assertTrue(policy.contains("localMoodRank < depressedMoodRank"));
        assertFalse(policy.contains("MicrophonePacketEvent"));
        assertFalse(policy.contains("VoicechatPlugin"));
        assertFalse(policy.contains("RoleNameRenderer"));
        assertFalse(policy.contains("SHUFFLED_PLAYER_ENTRIES_CACHE"));
    }

    @Test
    void insaneParanoidKillerRemainsDisabledAndOutOfRuntimeSelection() throws IOException {
        NoellesRoleCatalog.Entry entry = NoellesRoleCatalog.find(InsaneParanoidKillerPolicy.ROLE_ID).orElseThrow();
        Set<net.minecraft.util.Identifier> runtimeIds = NoellesRoleCatalog.runtimeSelectionDefinitions().stream()
                .map(org.caecorthus.strawcraft.role.StrawRoleDefinition::id)
                .collect(java.util.stream.Collectors.toSet());
        String catalog = Files.readString(CATALOG, StandardCharsets.UTF_8);

        assertEquals(NoellesRoleCatalog.Readiness.DISABLED, entry.readiness());
        assertFalse(entry.firstRoundEligible());
        assertFalse(entry.isRuntimeReady());
        assertTrue(NoellesRoleCatalog.runtimeSelectionDisabledIds().contains(entry.id()));
        assertFalse(runtimeIds.contains(entry.id()));
        assertTrue(catalog.contains("disabledKiller(\"the_insane_damned_paranoid_killer\")"));
    }

    @Test
    void noRuntimeVoicePluginOrClientHallucinationHookIsWired() throws IOException {
        String initializer = Files.readString(INITIALIZER, StandardCharsets.UTF_8);
        String client = Files.readString(CLIENT, StandardCharsets.UTF_8);
        String mixinConfig = Files.readString(MIXIN_CONFIG, StandardCharsets.UTF_8);
        String fabricMod = Files.readString(FABRIC_MOD, StandardCharsets.UTF_8);

        assertFalse(initializer.contains("InsaneParanoidKiller"));
        assertFalse(client.contains("InsaneParanoidKiller"));
        assertFalse(client.contains("insaneSeesMorphs"));
        assertFalse(mixinConfig.contains("RoleNameRenderer"));
        assertFalse(mixinConfig.contains("Morphling"));
        assertFalse(fabricMod.contains("VoicechatPlugin"));
        assertFalse(fabricMod.contains("NoellesrolesVoiceChatPlugin"));

        for (Path path : mainJavaSources()) {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            assertFalse(source.contains("MicrophonePacketEvent"), path + " must not wire Simple Voice Chat microphone hooks");
            assertFalse(source.contains("VoicechatPlugin"), path + " must not wire Simple Voice Chat plugins");
            assertFalse(source.contains("insaneSeesMorphs"), path + " must not wire Spark client hallucination config");
            assertFalse(source.contains("SHUFFLED_PLAYER_ENTRIES_CACHE"), path + " must not depend on Spark client caches");
        }
    }

    @Test
    void foundationAvoidsSparkNoellesTrainMurderMysteryAndParoxRuntimeDependencies() throws IOException {
        String policy = Files.readString(POLICY, StandardCharsets.UTF_8);
        String trainmurdermysteryImport = "import org." + "trainmurdermystery";
        String noellesRolesImport = "import org." + "noellesroles";
        String xruiNoellesRuntime = "XruiDD." + "NoellesRoles";
        String paroxWathe = "wathe-" + "Parox";

        assertFalse(policy.contains(trainmurdermysteryImport));
        assertFalse(policy.contains(noellesRolesImport));
        assertFalse(policy.contains(xruiNoellesRuntime));
        assertFalse(policy.contains(paroxWathe));
    }

    private static Iterable<Path> mainJavaSources() throws IOException {
        try (Stream<Path> sources = Files.walk(MAIN_ROOT)) {
            return sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }
}
