package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BomberTimedBombPolicyTest {
    @Test
    void bomberCanAttachTimedBombToAliveNonSelfTargetInRunningRound() {
        assertEquals(BomberTimedBombPolicy.AttachResult.ALLOWED, BomberTimedBombPolicy.validateAttach(input(
                true,
                true,
                true,
                true,
                false,
                false,
                true
        )));
    }

    @Test
    void attachRejectsUnsafeActorTargetRoundAndCooldownStates() {
        assertEquals(BomberTimedBombPolicy.AttachResult.ROUND_NOT_RUNNING, BomberTimedBombPolicy.validateAttach(input(false, true, true, true, false, false, true)));
        assertEquals(BomberTimedBombPolicy.AttachResult.NOT_BOMBER, BomberTimedBombPolicy.validateAttach(input(true, false, true, true, false, false, true)));
        assertEquals(BomberTimedBombPolicy.AttachResult.ACTOR_DEAD, BomberTimedBombPolicy.validateAttach(input(true, true, false, true, false, false, true)));
        assertEquals(BomberTimedBombPolicy.AttachResult.TARGET_DEAD, BomberTimedBombPolicy.validateAttach(input(true, true, true, false, false, false, true)));
        assertEquals(BomberTimedBombPolicy.AttachResult.SELF_TARGET, BomberTimedBombPolicy.validateAttach(input(true, true, true, true, true, false, true)));
        assertEquals(BomberTimedBombPolicy.AttachResult.TARGET_ALREADY_CARRIER, BomberTimedBombPolicy.validateAttach(input(true, true, true, true, false, true, true)));
        assertEquals(BomberTimedBombPolicy.AttachResult.COOLDOWN, BomberTimedBombPolicy.validateAttach(input(true, true, true, true, false, false, false)));
    }

    @Test
    void createdBombRecordsOwnerCarrierDeadlineAndInitialPhase() {
        UUID owner = UUID.randomUUID();
        UUID carrier = UUID.randomUUID();

        NoellesRoleState.TimedBomb bomb = BomberTimedBombPolicy.createBomb(owner, carrier, 100L);

        assertEquals(owner, bomb.ownerUuid());
        assertEquals(carrier, bomb.carrierUuid());
        assertEquals(100L + BomberTimedBombPolicy.FUSE_TICKS, bomb.fuseDeadlineTick());
        assertEquals(NoellesRoleState.TimedBombPhase.ARMED, bomb.phase());
        assertEquals(0L, bomb.transferCooldownDeadlineTick());
    }

    @Test
    void expiryWaitsUntilDeadlineThenKillsOnlyMatchingAliveRunningCarrier() {
        UUID carrier = UUID.randomUUID();
        NoellesRoleState.TimedBomb bomb = BomberTimedBombPolicy.createBomb(UUID.randomUUID(), carrier, 100L);

        assertEquals(BomberTimedBombPolicy.ExpiryResult.WAIT,
                BomberTimedBombPolicy.expiryResult(bomb, carrier, true, true, bomb.fuseDeadlineTick() - 1));
        assertEquals(BomberTimedBombPolicy.ExpiryResult.KILL_CARRIER,
                BomberTimedBombPolicy.expiryResult(bomb, carrier, true, true, bomb.fuseDeadlineTick()));
        assertEquals(BomberTimedBombPolicy.ExpiryResult.CLEAR_ONLY,
                BomberTimedBombPolicy.expiryResult(bomb, UUID.randomUUID(), true, true, bomb.fuseDeadlineTick()));
        assertEquals(BomberTimedBombPolicy.ExpiryResult.CLEAR_ONLY,
                BomberTimedBombPolicy.expiryResult(bomb, carrier, false, true, bomb.fuseDeadlineTick()));
        assertEquals(BomberTimedBombPolicy.ExpiryResult.CLEAR_ONLY,
                BomberTimedBombPolicy.expiryResult(bomb, carrier, true, false, bomb.fuseDeadlineTick()));
    }

    private static BomberTimedBombPolicy.AttachInput input(
            boolean roundRunning,
            boolean bomberRole,
            boolean actorAlive,
            boolean targetAlive,
            boolean selfTarget,
            boolean targetAlreadyCarrier,
            boolean cooldownReady
    ) {
        return new BomberTimedBombPolicy.AttachInput(
                roundRunning,
                bomberRole,
                actorAlive,
                targetAlive,
                selfTarget,
                targetAlreadyCarrier,
                cooldownReady
        );
    }
}
