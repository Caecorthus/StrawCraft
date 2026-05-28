package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.ASM9;

class SurvivalMasterCountdownRuntimeArchitectureTest {
    private static final Path RUNTIME =
            Path.of("src/main/java/org/caecorthus/strawcraft/SurvivalMasterCountdownRuntime.java");

    @Test
    void runtimeCompletesThroughOfficialPassengerRoundEndPath() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("GameRoundEndComponent.KEY.get(world)"));
        assertTrue(source.contains("setRoundEndData(world.getPlayers(), GameFunctions.WinStatus.PASSENGERS)"));
        assertTrue(source.contains("GameFunctions.stopGame(world)"));
        assertTrue(source.contains("ServerTickEvents.END_SERVER_TICK.register"));
    }

    @Test
    void runtimeResetsOnOfficialWatheRoundBoundaries() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("GameEvents.ON_FINISH_INITIALIZE.register"));
        assertTrue(source.contains("GameEvents.ON_FINISH_FINALIZE.register"));
        assertTrue(source.contains("resetWorld(serverWorld)"));
    }

    @Test
    void runtimeInitializesStartingKillersFromRewrittenRoleMap() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertTrue(source.contains("countStartingKillers(game.getRoles().values())"));
        assertFalse(source.contains("getAllKillerTeamPlayers()"));
    }

    @Test
    void runtimeDoesNotUseSparkWinConditionOrNoellesRuntimeImports() throws IOException {
        String source = Files.readString(RUNTIME, StandardCharsets.UTF_8);

        assertFalse(source.contains("CheckWinCondition"));
        assertFalse(source.contains("XruiDD"));
        assertFalse(source.contains("dev.doctor4t.noellesroles"));
        assertFalse(source.contains("org.noellesroles"));
    }

    @Test
    void strawCraftRegistersSurvivalMasterRuntime() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/StrawCraft.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(source.contains("SurvivalMasterCountdownRuntime.registerEvents()"));
    }

    @Test
    void strawCraftRegistersNoellesRewriteBeforeSurvivalMasterRuntime() throws IOException {
        List<String> calls = onInitializeMethodCalls();

        int bridgeRegistration = calls.indexOf("org/caecorthus/strawcraft/WatheOfficialBridge.register()V");
        int survivalRegistration =
                calls.indexOf("org/caecorthus/strawcraft/SurvivalMasterCountdownRuntime.registerEvents()V");
        assertTrue(bridgeRegistration >= 0);
        assertTrue(survivalRegistration > bridgeRegistration);
    }

    private static List<String> onInitializeMethodCalls() throws IOException {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("org/caecorthus/strawcraft/StrawCraft.class")) {
            if (input == null) {
                throw new AssertionError("Compiled StrawCraft class was not available to inspect.");
            }
            List<String> calls = new ArrayList<>();
            new ClassReader(input).accept(new ClassVisitor(ASM9) {
                @Override
                public MethodVisitor visitMethod(
                        int access,
                        String name,
                        String descriptor,
                        String signature,
                        String[] exceptions
                ) {
                    if (!"onInitialize".equals(name) || !"()V".equals(descriptor)) {
                        return null;
                    }
                    return new MethodVisitor(ASM9) {
                        @Override
                        public void visitMethodInsn(
                                int opcode,
                                String owner,
                                String name,
                                String descriptor,
                                boolean isInterface
                        ) {
                            calls.add(owner + "." + name + descriptor);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return calls;
        }
    }
}
