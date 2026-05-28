package org.caecorthus.strawcraft.mixincontract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialWinHookMixinContractTest {
    private static final String MURDER_GAME_MODE = "dev.doctor4t.wathe.game.gamemode.MurderGameMode";
    private static final String GAME_ROUND_END_COMPONENT = "dev.doctor4t.wathe.cca.GameRoundEndComponent";
    private static final String TICK_SERVER_GAME_LOOP_DESCRIPTOR =
            "(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;)V";
    private static final String SET_ROUND_END_DATA_DESCRIPTOR =
            "(Ljava/util/List;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)V";

    @Test
    void officialMurderGameModeStillSetsRoundEndDataFromTheServerLoop() {
        assertTrue(MixinTargetBytecode.hasMethod(
                MURDER_GAME_MODE,
                "tickServerGameLoop",
                TICK_SERVER_GAME_LOOP_DESCRIPTOR
        ));
        assertEquals(1, MixinTargetBytecode.countMethodInvocations(
                MURDER_GAME_MODE,
                "tickServerGameLoop",
                TICK_SERVER_GAME_LOOP_DESCRIPTOR,
                "dev/doctor4t/wathe/cca/GameRoundEndComponent",
                "setRoundEndData",
                SET_ROUND_END_DATA_DESCRIPTOR
        ));
    }

    @Test
    void officialMurderGameModeKeepsWinStatusLocalBeforeRoundEndData() {
        assertEquals(3, MixinTargetBytecode.localVariableSlot(
                MURDER_GAME_MODE,
                "tickServerGameLoop",
                TICK_SERVER_GAME_LOOP_DESCRIPTOR,
                "winStatus",
                "Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;"
        ));
    }

    @Test
    void strawCraftWinHookMixinTargetsOfficialMurderGameModeBeforeRoundEndData() {
        MixinTargetBytecode.MixinClassInspection mixin = MixinTargetBytecode.inspectMixinClass(
                "org.caecorthus.strawcraft.mixin.MurderGameModeMixin",
                MURDER_GAME_MODE
        );

        assertTrue(mixin.classPresent());
        assertTrue(mixin.targetsExpectedOwner());
        assertTrue(mixin.injections().stream().anyMatch(injection ->
                injection.targetsMethod("tickServerGameLoop")
                        && injection.targetsAt("Ldev/doctor4t/wathe/cca/GameRoundEndComponent;setRoundEndData"
                        + SET_ROUND_END_DATA_DESCRIPTOR)
        ), () -> "Expected soft injection before GameRoundEndComponent#setRoundEndData.\n"
                + mixin.describeInjections());
    }

    @Test
    void officialRoundEndComponentStillExposesDidWinForLooseEndPatch() {
        assertTrue(MixinTargetBytecode.hasMethod(
                GAME_ROUND_END_COMPONENT,
                "didWin",
                "(Ljava/util/UUID;)Z"
        ));
    }

    @Test
    void strawCraftRoundEndMixinTargetsOfficialDidWin() {
        MixinTargetBytecode.MixinClassInspection mixin = MixinTargetBytecode.inspectMixinClass(
                "org.caecorthus.strawcraft.mixin.GameRoundEndComponentMixin",
                GAME_ROUND_END_COMPONENT
        );

        assertTrue(mixin.classPresent());
        assertTrue(mixin.targetsExpectedOwner());
        assertTrue(mixin.hasSoftInjectionFor("didWin"), () -> "Expected soft didWin injection.\n"
                + mixin.describeInjections());
    }
}
