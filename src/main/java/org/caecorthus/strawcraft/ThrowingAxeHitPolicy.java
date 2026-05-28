package org.caecorthus.strawcraft;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ThrowingAxeHitPolicy {
    private final Set<UUID> hitTargets = new HashSet<>();

    public Result evaluateHit(Input input) {
        Objects.requireNonNull(input, "input");
        if (input.ownerUuid().equals(input.targetUuid())) {
            return Result.IGNORE_OWNER;
        }
        if (!input.targetAlive() || !input.targetSurvival()) {
            return Result.IGNORE_INVALID_TARGET;
        }
        if (!hitTargets.add(input.targetUuid())) {
            return Result.IGNORE_REPEAT_HIT;
        }
        return Result.KILL_TARGET;
    }

    public boolean hasHit(UUID targetUuid) {
        return hitTargets.contains(targetUuid);
    }

    public int hitCount() {
        return hitTargets.size();
    }

    public record Input(UUID ownerUuid, UUID targetUuid, boolean targetAlive, boolean targetSurvival) {
        public Input {
            Objects.requireNonNull(ownerUuid, "ownerUuid");
            Objects.requireNonNull(targetUuid, "targetUuid");
        }
    }

    public enum Result {
        IGNORE_OWNER(false),
        IGNORE_INVALID_TARGET(false),
        IGNORE_REPEAT_HIT(false),
        KILL_TARGET(true);

        private final boolean killsTarget;

        Result(boolean killsTarget) {
            this.killsTarget = killsTarget;
        }

        public boolean killsTarget() {
            return killsTarget;
        }
    }
}
