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
        saved.tryBeginAbilityCooldown("detective_investigate", 100L, 1800);

        NbtCompound nbt = new NbtCompound();
        saved.writeToNbt(nbt);

        NoellesRoleState loaded = new NoellesRoleState();
        loaded.readFromNbt(nbt);

        assertTrue(loaded.hasFlag("spirit_has_revealed"));
        assertFalse(loaded.hasFlag("morphling_disguised"));
        assertEquals(OptionalLong.of(260L), loaded.getTimestamp("taotie_last_swallow"));
        assertEquals(1800, loaded.getRemainingAbilityCooldown("detective_investigate", 100L));
    }

    @Test
    void resetClearsRoundScopedRoleState() {
        NoellesRoleState state = new NoellesRoleState();
        state.setFlag("spirit_has_revealed", true);
        state.setFlag(ProfessorIronManProtection.PROTECTION_FLAG, true);
        state.setTimestamp("taotie_last_swallow", 260L);
        state.tryBeginAbilityCooldown("detective_investigate", 100L, 1800);
        state.setVoodooBondedTarget(UUID.randomUUID());
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
        assertFalse(state.isAbilityOnCooldown("detective_investigate", 101L));
        assertTrue(state.voodooBondedTarget().isEmpty());
        assertTrue(state.demonHunterFrenziedPlayers().isEmpty());
        assertTrue(state.neutralWinClaims().isEmpty());
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
}
