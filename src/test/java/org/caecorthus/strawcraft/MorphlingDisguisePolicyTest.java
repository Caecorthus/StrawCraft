package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorphlingDisguisePolicyTest {
    @Test
    void allowsRunningLiveMorphlingToDisguiseAsLiveAssignedTargetWhenReady() {
        assertEquals(MorphlingDisguisePolicy.ValidationResult.ALLOWED,
                MorphlingDisguisePolicy.validateStart(validStart()));
    }

    @Test
    void rejectsForgedOrStaleServerStateForStartingDisguise() {
        assertBlocked(validStart().withRoundRunning(false));
        assertBlocked(validStart().withMorphlingRole(false));
        assertBlocked(validStart().withMorphlingAlive(false));
        assertBlocked(validStart().withMorphlingSwallowed(true));
        assertBlocked(validStart().withTargetPresent(false));
        assertBlocked(validStart().withSelfTarget(true));
        assertBlocked(validStart().withTargetAssigned(false));
        assertBlocked(validStart().withTargetAlive(false));
        assertBlocked(validStart().withTargetSwallowed(true));
        assertBlocked(validStart().withSameWorld(false));
        assertBlocked(validStart().withCooldownReady(false));
        assertBlocked(validStart().withRecoveryReady(false));
    }

    @Test
    void startStopToggleAndTickMirrorServerOwnedMorphTimers() {
        UUID target = UUID.randomUUID();

        NoellesRoleState.MorphlingDisguiseState started = MorphlingDisguisePolicy.startMorph(
                NoellesRoleState.MorphlingDisguiseState.empty(),
                target,
                100L
        );

        assertEquals(Optional.of(target), started.disguiseUuid());
        assertEquals(MorphlingDisguisePolicy.ACTIVE_TICKS, started.morphTicks());
        assertEquals(800L, started.activeDeadlineTick());
        assertTrue(MorphlingDisguisePolicy.isActive(started));

        NoellesRoleState.MorphlingDisguiseState toggled = MorphlingDisguisePolicy.toggleCorpseMode(started);
        assertTrue(toggled.corpseMode());

        NoellesRoleState.MorphlingDisguiseState ticked = MorphlingDisguisePolicy.tick(toggled, true);
        assertEquals(MorphlingDisguisePolicy.ACTIVE_TICKS - 1, ticked.morphTicks());
        assertEquals(Optional.of(target), ticked.disguiseUuid());
        assertTrue(ticked.corpseMode());

        NoellesRoleState.MorphlingDisguiseState stopped = MorphlingDisguisePolicy.stopMorph(ticked);
        assertTrue(stopped.disguiseUuid().isEmpty());
        assertEquals(-MorphlingDisguisePolicy.RECOVERY_TICKS, stopped.morphTicks());
        assertEquals(0L, stopped.activeDeadlineTick());
        assertTrue(stopped.corpseMode());

        NoellesRoleState.MorphlingDisguiseState recovering = MorphlingDisguisePolicy.tick(stopped, true);
        assertEquals(-MorphlingDisguisePolicy.RECOVERY_TICKS + 1, recovering.morphTicks());

        NoellesRoleState.MorphlingDisguiseState ready = MorphlingDisguisePolicy.tick(
                new NoellesRoleState.MorphlingDisguiseState(Optional.empty(), -1, 0L, false),
                true
        );
        assertEquals(NoellesRoleState.MorphlingDisguiseState.empty(), ready);
    }

    @Test
    void activeMorphStopsWhenTargetIsGone() {
        UUID target = UUID.randomUUID();
        NoellesRoleState.MorphlingDisguiseState started = MorphlingDisguisePolicy.startMorph(
                NoellesRoleState.MorphlingDisguiseState.empty(),
                target,
                100L
        );

        NoellesRoleState.MorphlingDisguiseState stopped = MorphlingDisguisePolicy.tick(started, false);

        assertTrue(stopped.disguiseUuid().isEmpty());
        assertEquals(-MorphlingDisguisePolicy.RECOVERY_TICKS, stopped.morphTicks());
    }

    private static void assertBlocked(MorphlingDisguisePolicy.StartInput input) {
        assertTrue(MorphlingDisguisePolicy.validateStart(input).blocked());
    }

    private static MorphlingDisguisePolicy.StartInput validStart() {
        return new MorphlingDisguisePolicy.StartInput(
                true,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true
        );
    }
}
