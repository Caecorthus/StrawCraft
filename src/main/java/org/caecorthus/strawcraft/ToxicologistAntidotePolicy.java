package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;

public final class ToxicologistAntidotePolicy {
    public static final double MAX_CURE_DISTANCE = 4.0D;
    public static final double MAX_CURE_DISTANCE_SQUARED = MAX_CURE_DISTANCE * MAX_CURE_DISTANCE;

    private ToxicologistAntidotePolicy() {
    }

    public static CureTarget selectCureTarget(Input input) {
        if (!StrawRoleMeaning.receivesToxicologistPoisonVisibility(input.role())
                || !input.carrierAlive()) {
            return CureTarget.NONE;
        }
        if (input.selfPoisoned()) {
            return CureTarget.SELF;
        }
        if (input.nearbyTargetPresent()
                && input.nearbyTargetAlive()
                && input.nearbyTargetPoisoned()
                && input.nearbyTargetSquaredDistance() <= MAX_CURE_DISTANCE_SQUARED) {
            return CureTarget.NEARBY_PLAYER;
        }
        return CureTarget.NONE;
    }

    public record Input(
            Role role,
            boolean carrierAlive,
            boolean selfPoisoned,
            boolean nearbyTargetPresent,
            boolean nearbyTargetAlive,
            boolean nearbyTargetPoisoned,
            double nearbyTargetSquaredDistance
    ) {
    }

    public enum CureTarget {
        NONE,
        SELF,
        NEARBY_PLAYER
    }
}
