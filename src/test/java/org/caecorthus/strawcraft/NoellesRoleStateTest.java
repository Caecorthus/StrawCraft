package org.caecorthus.strawcraft;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesRoleStateTest {
    @Test
    void namedAbilityCooldownsUseAbsoluteDeadlines() {
        NoellesRoleState state = new NoellesRoleState();

        assertTrue(state.tryBeginAbilityCooldown("detective_investigate", 100L, 1800));
        assertTrue(state.isAbilityOnCooldown("detective_investigate", 101L));
        assertEquals(1799, state.getRemainingAbilityCooldown("detective_investigate", 101L));

        assertFalse(state.tryBeginAbilityCooldown("detective_investigate", 101L, 1800));
        assertTrue(state.tryBeginAbilityCooldown("professor_iron_man", 101L, 200));
        assertTrue(state.tryBeginAbilityCooldown("detective_investigate", 1900L, 1800));
    }

    @Test
    void flagsTimestampsAndCooldownsRoundTripThroughNbt() {
        NoellesRoleState saved = new NoellesRoleState();
        saved.setFlag("spirit_has_revealed", true);
        saved.setFlag("morphling_disguised", false);
        saved.setTimestamp("taotie_last_swallow", 260L);
        saved.setSpiritualistProjection(new NoellesRoleState.SpiritualistProjection(1.5D, 64.0D, -3.25D, 320L));
        saved.tryBeginAbilityCooldown("detective_investigate", 100L, 1800);

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertTrue(loaded.hasFlag("spirit_has_revealed"));
        assertFalse(loaded.hasFlag("morphling_disguised"));
        assertEquals(OptionalLong.of(260L), loaded.getTimestamp("taotie_last_swallow"));
        NoellesRoleState.SpiritualistProjection projection = loaded.spiritualistProjection().orElseThrow();
        assertEquals(1.5D, projection.bodyX());
        assertEquals(64.0D, projection.bodyY());
        assertEquals(-3.25D, projection.bodyZ());
        assertEquals(320L, projection.startedAtTick());
        assertEquals(1800, loaded.getRemainingAbilityCooldown("detective_investigate", 100L));
    }

    @Test
    void resetClearsRoundScopedRoleState() {
        NoellesRoleState state = new NoellesRoleState();
        state.setFlag("spirit_has_revealed", true);
        state.setFlag(ProfessorIronManProtection.PROTECTION_FLAG, true);
        state.setTimestamp("taotie_last_swallow", 260L);
        state.setSpiritualistProjection(new NoellesRoleState.SpiritualistProjection(1.0D, 2.0D, 3.0D, 120L));
        state.tryBeginAbilityCooldown("detective_investigate", 100L, 1800);
        AssassinGuessPolicy.resetRoundState(state, 6, 100L);
        state.setVoodooBondedTarget(UUID.randomUUID());
        state.setPathogenInfectedBy(UUID.randomUUID());
        state.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                false,
                true,
                JesterWinPolicy.STASIS_TICKS,
                false,
                0,
                Optional.of(UUID.randomUUID()),
                1.0D,
                2.0D,
                3.0D
        ));
        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                StrawCraft.id("jester"),
                StrawCraft.id("jester_killed"),
                Optional.of(UUID.randomUUID()),
                120L
        ));

        state.reset();

        assertFalse(state.hasFlag("spirit_has_revealed"));
        assertFalse(state.hasFlag(ProfessorIronManProtection.PROTECTION_FLAG));
        assertEquals(OptionalLong.empty(), state.getTimestamp("taotie_last_swallow"));
        assertTrue(state.spiritualistProjection().isEmpty());
        assertFalse(state.isAbilityOnCooldown("detective_investigate", 101L));
        assertEquals(0, AssassinGuessPolicy.guessesRemaining(state));
        assertFalse(state.isAbilityOnCooldown(AssassinGuessPolicy.ABILITY_ID, 101L));
        assertTrue(state.voodooBondedTarget().isEmpty());
        assertTrue(state.pathogenInfectedBy().isEmpty());
        assertTrue(state.demonHunterFrenziedPlayers().isEmpty());
        assertFalse(state.jesterMomentState().hasState());
        assertTrue(state.neutralWinClaims().isEmpty());
    }

    @Test
    void assassinGuessStateResetsAndRoundTripsThroughNbt() {
        NoellesRoleState saved = new NoellesRoleState();
        AssassinGuessPolicy.resetRoundState(saved, 9, 400L);
        AssassinGuessPolicy.useGuess(saved, 1600L);

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertEquals(8, AssassinGuessPolicy.guessesRemaining(loaded));
        assertEquals(9, AssassinGuessPolicy.maxGuesses(loaded));
        assertTrue(loaded.isAbilityOnCooldown(AssassinGuessPolicy.ABILITY_ID, 1600L));
        assertEquals(AssassinGuessPolicy.COOLDOWN_TICKS, loaded.getRemainingAbilityCooldown(AssassinGuessPolicy.ABILITY_ID, 1600L));
    }

    @Test
    void neutralWinClaimsRoundTripThroughNbt() {
        UUID opponent = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        saved.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                StrawCraft.id("jester"),
                StrawCraft.id("jester_killed"),
                Optional.of(opponent),
                240L
        ));

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        NoellesRoleState.NeutralWinClaim claim = loaded.neutralWinClaim(StrawCraft.id("jester")).orElseThrow();
        assertEquals(StrawCraft.id("jester"), claim.roleId());
        assertEquals(StrawCraft.id("jester_killed"), claim.trigger());
        assertEquals(Optional.of(opponent), claim.opponentUuid());
        assertEquals(240L, claim.gameTime());
    }

    @Test
    void jesterMomentStateRoundTripsThroughNbt() {
        UUID targetKiller = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        saved.setJesterMomentState(new NoellesRoleState.JesterMomentState(
                true,
                false,
                0,
                true,
                JesterRuntime.PSYCHO_TICKS,
                Optional.of(targetKiller),
                12.5D,
                65.0D,
                -7.25D
        ));

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        NoellesRoleState.JesterMomentState state = loaded.jesterMomentState();
        assertTrue(state.won());
        assertTrue(state.inPsychoMode());
        assertEquals(JesterRuntime.PSYCHO_TICKS, state.psychoModeTicks());
        assertEquals(Optional.of(targetKiller), state.targetKiller());
        assertEquals(12.5D, state.stasisX());
        assertEquals(65.0D, state.stasisY());
        assertEquals(-7.25D, state.stasisZ());
    }

    @Test
    void reporterMarkedTargetStoresExactlyOneUuidAndRoundTripsThroughNbt() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();

        saved.setReporterMarkedTarget(firstTarget);
        saved.setReporterMarkedTarget(secondTarget);

        assertEquals(Optional.of(secondTarget), saved.reporterMarkedTarget());
        assertEquals(1, saved.uuidSet("reporter_marked_target").size());

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);
        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertEquals(Optional.of(secondTarget), loaded.reporterMarkedTarget());
        loaded.clearReporterMarkedTarget();
        assertTrue(loaded.reporterMarkedTarget().isEmpty());
    }

    @Test
    void serialKillerCurrentTargetStoresExactlyOneUuidAndRoundTripsThroughNbt() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();

        saved.setSerialKillerCurrentTarget(firstTarget);
        saved.setSerialKillerCurrentTarget(secondTarget);

        assertEquals(Optional.of(secondTarget), saved.serialKillerCurrentTarget());
        assertEquals(1, saved.uuidSet("serial_killer_current_target").size());

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);
        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertEquals(Optional.of(secondTarget), loaded.serialKillerCurrentTarget());
        loaded.clearSerialKillerCurrentTarget();
        assertTrue(loaded.serialKillerCurrentTarget().isEmpty());
    }

    @Test
    void taotieSwallowedPlayersAndPerTargetOwnerRoundTripThroughNbt() {
        UUID taotie = UUID.randomUUID();
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        NoellesRoleState savedTaotie = new NoellesRoleState();
        NoellesRoleState savedTarget = new NoellesRoleState();

        assertTrue(savedTaotie.trackTaotieSwallowedPlayer(firstTarget));
        assertTrue(savedTaotie.trackTaotieSwallowedPlayer(secondTarget));
        assertFalse(savedTaotie.trackTaotieSwallowedPlayer(firstTarget));
        savedTarget.setTaotieSwallowedBy(taotie);

        NbtCompound taotieNbt = new NbtCompound();
        savedTaotie.writeToNbt(taotieNbt);
        NbtCompound targetNbt = new NbtCompound();
        savedTarget.writeToNbt(targetNbt);

        NoellesRoleState loadedTaotie = new NoellesRoleState();
        loadedTaotie.readFromNbt(taotieNbt);
        NoellesRoleState loadedTarget = new NoellesRoleState();
        loadedTarget.readFromNbt(targetNbt);

        assertEquals(java.util.Set.of(firstTarget, secondTarget), loadedTaotie.taotieSwallowedPlayers());
        assertTrue(loadedTaotie.untrackTaotieSwallowedPlayer(firstTarget));
        assertEquals(java.util.Set.of(secondTarget), loadedTaotie.taotieSwallowedPlayers());
        assertEquals(Optional.of(taotie), loadedTarget.taotieSwallowedBy());
        assertTrue(loadedTarget.isTaotieSwallowed());

        loadedTarget.clearTaotieSwallowedBy();
        assertTrue(loadedTarget.taotieSwallowedBy().isEmpty());
        assertFalse(loadedTarget.isTaotieSwallowed());
    }

    @Test
    void voodooBondedTargetStoresExactlyOneUuidAndRoundTripsThroughNbt() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        saved.setVoodooBondedTarget(firstTarget);
        saved.setVoodooBondedTarget(secondTarget);

        assertEquals(Optional.of(secondTarget), saved.voodooBondedTarget());
        assertEquals(1, saved.uuidSet("voodoo_bonded_target").size());

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);
        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertEquals(Optional.of(secondTarget), loaded.voodooBondedTarget());
        loaded.clearVoodooBondedTarget();
        assertTrue(loaded.voodooBondedTarget().isEmpty());
    }

    @Test
    void pathogenInfectionStoresInfectingPathogenAndRoundTripsThroughNbt() {
        UUID firstPathogen = UUID.randomUUID();
        UUID secondPathogen = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        saved.setPathogenInfectedBy(firstPathogen);
        saved.setPathogenInfectedBy(secondPathogen);

        assertEquals(Optional.of(secondPathogen), saved.pathogenInfectedBy());

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);
        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertEquals(Optional.of(secondPathogen), loaded.pathogenInfectedBy());
        loaded.clearPathogenInfection();
        assertTrue(loaded.pathogenInfectedBy().isEmpty());
    }

    @Test
    void demonHunterFrenziedPlayersTrackIndependentlyAndRoundTripThroughNbt() {
        UUID firstFrenzied = UUID.randomUUID();
        UUID secondFrenzied = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();

        assertTrue(saved.trackDemonHunterFrenziedPlayer(firstFrenzied));
        assertTrue(saved.trackDemonHunterFrenziedPlayer(secondFrenzied));
        assertFalse(saved.trackDemonHunterFrenziedPlayer(firstFrenzied));
        assertTrue(saved.hasDemonHunterFrenziedPlayer(firstFrenzied));
        assertEquals(2, saved.demonHunterFrenziedPlayers().size());

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);
        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertTrue(loaded.hasDemonHunterFrenziedPlayer(firstFrenzied));
        assertTrue(loaded.hasDemonHunterFrenziedPlayer(secondFrenzied));
        assertTrue(loaded.untrackDemonHunterFrenziedPlayer(firstFrenzied));
        assertFalse(loaded.hasDemonHunterFrenziedPlayer(firstFrenzied));
        assertTrue(loaded.hasDemonHunterFrenziedPlayer(secondFrenzied));
    }

    @Test
    void timedBombRoundTripsThroughNbtAndResetClearsIt() {
        UUID owner = UUID.randomUUID();
        UUID carrier = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        saved.setTimedBomb(new NoellesRoleState.TimedBomb(
                owner,
                carrier,
                900L,
                NoellesRoleState.TimedBombPhase.ARMED,
                720L
        ));

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);
        NoellesRoleState.TimedBomb bomb = loaded.timedBomb().orElseThrow();
        assertEquals(owner, bomb.ownerUuid());
        assertEquals(carrier, bomb.carrierUuid());
        assertEquals(900L, bomb.fuseDeadlineTick());
        assertEquals(NoellesRoleState.TimedBombPhase.ARMED, bomb.phase());
        assertEquals(720L, bomb.transferCooldownDeadlineTick());

        loaded.reset();

        assertTrue(loaded.timedBomb().isEmpty());
    }

    @Test
    void recallerRecallPointRoundTripsThroughNbtAndResetClearsIt() {
        NoellesRoleState saved = new NoellesRoleState();
        saved.setRecallerRecallPoint(new NoellesRoleState.RecallPoint(
                StrawCraft.id("arena"),
                10.5D,
                64.0D,
                -8.25D,
                90.0F,
                15.0F
        ));

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        NoellesRoleState.RecallPoint point = loaded.recallerRecallPoint().orElseThrow();
        assertEquals(StrawCraft.id("arena"), point.worldId());
        assertEquals(10.5D, point.x());
        assertEquals(64.0D, point.y());
        assertEquals(-8.25D, point.z());
        assertEquals(90.0F, point.yaw());
        assertEquals(15.0F, point.pitch());

        loaded.reset();
        assertTrue(loaded.recallerRecallPoint().isEmpty());
    }

    @Test
    void writingEmptyRecallerRecallPointClearsStaleNbtKey() {
        NoellesRoleState withPoint = new NoellesRoleState();
        withPoint.setRecallerRecallPoint(new NoellesRoleState.RecallPoint(
                StrawCraft.id("arena"),
                1.0D,
                2.0D,
                3.0D,
                4.0F,
                5.0F
        ));

        NbtCompound nbt = new NbtCompound();
        withPoint.writeToNbt(nbt);

        NoellesRoleState empty = new NoellesRoleState();
        empty.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertTrue(loaded.recallerRecallPoint().isEmpty());
    }

    @Test
    void writingEmptyTimedBombStateClearsStaleNbtKey() {
        UUID owner = UUID.randomUUID();
        UUID carrier = UUID.randomUUID();
        NoellesRoleState withBomb = new NoellesRoleState();
        withBomb.setTimedBomb(new NoellesRoleState.TimedBomb(
                owner,
                carrier,
                900L,
                NoellesRoleState.TimedBombPhase.ARMED,
                720L
        ));
        NbtCompound nbt = new NbtCompound();
        withBomb.writeToNbt(nbt);

        NoellesRoleState empty = new NoellesRoleState();
        empty.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);
        assertTrue(loaded.timedBomb().isEmpty());
    }

    @Test
    void morphlingDisguiseStateRoundTripsThroughNbtAndResetClearsIt() {
        UUID disguise = UUID.randomUUID();
        NoellesRoleState saved = new NoellesRoleState();
        saved.setMorphlingDisguiseState(new NoellesRoleState.MorphlingDisguiseState(
                Optional.of(disguise),
                MorphlingDisguisePolicy.ACTIVE_TICKS,
                900L,
                true
        ));

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        NoellesRoleState.MorphlingDisguiseState state = loaded.morphlingDisguiseState();
        assertEquals(Optional.of(disguise), state.disguiseUuid());
        assertEquals(MorphlingDisguisePolicy.ACTIVE_TICKS, state.morphTicks());
        assertEquals(900L, state.activeDeadlineTick());
        assertTrue(state.corpseMode());

        loaded.reset();

        assertEquals(NoellesRoleState.MorphlingDisguiseState.empty(), loaded.morphlingDisguiseState());

        NoellesRoleState empty = new NoellesRoleState();
        empty.writeToNbt(nbt);
        loaded.readFromNbt(nbt);

        assertEquals(NoellesRoleState.MorphlingDisguiseState.empty(), loaded.morphlingDisguiseState());
    }
}
