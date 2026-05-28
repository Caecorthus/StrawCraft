package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToxicologistAntidotePolicyTest {
    @Test
    void toxicologistCanCureSelfWhenPoisoned() {
        assertEquals(ToxicologistAntidotePolicy.CureTarget.SELF, ToxicologistAntidotePolicy.selectCureTarget(
                input(true, true, false, false, false, ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED)
        ));
    }

    @Test
    void toxicologistCanCureNearbyPoisonedPlayer() {
        assertEquals(ToxicologistAntidotePolicy.CureTarget.NEARBY_PLAYER, ToxicologistAntidotePolicy.selectCureTarget(
                input(true, false, true, true, true, ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED)
        ));
    }

    @Test
    void nonPoisonedTargetsAreDenied() {
        assertEquals(ToxicologistAntidotePolicy.CureTarget.NONE, ToxicologistAntidotePolicy.selectCureTarget(
                input(true, false, true, true, false, ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED)
        ));
    }

    @Test
    void deadCarrierIsDenied() {
        ToxicologistAntidotePolicy.Input input = new ToxicologistAntidotePolicy.Input(
                toxicologist(),
                false,
                true,
                false,
                false,
                false,
                ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED
        );

        assertEquals(ToxicologistAntidotePolicy.CureTarget.NONE, ToxicologistAntidotePolicy.selectCureTarget(input));
    }

    @Test
    void missingDeadOrOutOfRangeNearbyTargetIsDenied() {
        assertEquals(ToxicologistAntidotePolicy.CureTarget.NONE, ToxicologistAntidotePolicy.selectCureTarget(
                input(true, false, false, true, true, ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED)
        ));
        assertEquals(ToxicologistAntidotePolicy.CureTarget.NONE, ToxicologistAntidotePolicy.selectCureTarget(
                input(true, false, true, false, true, ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED)
        ));
        assertEquals(ToxicologistAntidotePolicy.CureTarget.NONE, ToxicologistAntidotePolicy.selectCureTarget(
                input(true, false, true, true, true, ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED + 0.01D)
        ));
    }

    @Test
    void wrongRoleIsDenied() {
        ToxicologistAntidotePolicy.Input input = new ToxicologistAntidotePolicy.Input(
                NoellesRoleCatalog.find(StrawCraft.id("detective")).orElseThrow().watheRole(),
                true,
                true,
                false,
                false,
                false,
                ToxicologistAntidotePolicy.MAX_CURE_DISTANCE_SQUARED
        );

        assertEquals(ToxicologistAntidotePolicy.CureTarget.NONE, ToxicologistAntidotePolicy.selectCureTarget(input));
    }

    private static ToxicologistAntidotePolicy.Input input(
            boolean toxicologistRole,
            boolean selfPoisoned,
            boolean nearbyTargetPresent,
            boolean nearbyTargetAlive,
            boolean nearbyTargetPoisoned,
            double nearbyTargetSquaredDistance
    ) {
        return new ToxicologistAntidotePolicy.Input(
                toxicologistRole ? toxicologist() : NoellesRoleCatalog.find(StrawCraft.id("detective")).orElseThrow().watheRole(),
                true,
                selfPoisoned,
                nearbyTargetPresent,
                nearbyTargetAlive,
                nearbyTargetPoisoned,
                nearbyTargetSquaredDistance
        );
    }

    private static Role toxicologist() {
        return NoellesRoleCatalog.find(StrawCraft.id("toxicologist")).orElseThrow().watheRole();
    }
}
