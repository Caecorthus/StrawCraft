package org.caecorthus.strawcraft;

import java.util.UUID;

public final class BomberTimedBombPolicy {
    public static final int FUSE_TICKS = 30 * 20;
    public static final int ATTACH_COOLDOWN_TICKS = 5 * 20;

    private BomberTimedBombPolicy() {
    }

    public static AttachResult validateAttach(AttachInput input) {
        if (!input.roundRunning()) {
            return AttachResult.ROUND_NOT_RUNNING;
        }
        if (!input.bomberRole()) {
            return AttachResult.NOT_BOMBER;
        }
        if (!input.actorAlive()) {
            return AttachResult.ACTOR_DEAD;
        }
        if (!input.targetAlive()) {
            return AttachResult.TARGET_DEAD;
        }
        if (input.selfTarget()) {
            return AttachResult.SELF_TARGET;
        }
        if (input.targetAlreadyCarrier()) {
            return AttachResult.TARGET_ALREADY_CARRIER;
        }
        if (!input.cooldownReady()) {
            return AttachResult.COOLDOWN;
        }
        return AttachResult.ALLOWED;
    }

    public static NoellesRoleState.TimedBomb createBomb(UUID ownerUuid, UUID carrierUuid, long now) {
        return new NoellesRoleState.TimedBomb(
                ownerUuid,
                carrierUuid,
                now + FUSE_TICKS,
                NoellesRoleState.TimedBombPhase.ARMED,
                0L
        );
    }

    public static ExpiryResult expiryResult(NoellesRoleState.TimedBomb bomb, UUID carrierUuid, boolean roundRunning, boolean carrierAlive, long now) {
        if (bomb == null) {
            return ExpiryResult.NO_BOMB;
        }
        if (!roundRunning || !carrierAlive || !bomb.carrierUuid().equals(carrierUuid)) {
            return ExpiryResult.CLEAR_ONLY;
        }
        return now >= bomb.fuseDeadlineTick() ? ExpiryResult.KILL_CARRIER : ExpiryResult.WAIT;
    }

    public record AttachInput(
            boolean roundRunning,
            boolean bomberRole,
            boolean actorAlive,
            boolean targetAlive,
            boolean selfTarget,
            boolean targetAlreadyCarrier,
            boolean cooldownReady
    ) {
    }

    public enum AttachResult {
        ALLOWED,
        ROUND_NOT_RUNNING,
        NOT_BOMBER,
        ACTOR_DEAD,
        TARGET_DEAD,
        SELF_TARGET,
        TARGET_ALREADY_CARRIER,
        COOLDOWN
    }

    public enum ExpiryResult {
        NO_BOMB,
        WAIT,
        CLEAR_ONLY,
        KILL_CARRIER
    }
}
