package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.role.StrawFaction;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScavengerHiddenBodyVisibilityTest {
    @Test
    void unhiddenBodiesStayVisibleToEveryone() {
        assertTrue(ScavengerHiddenBodyVisibility.canSeeBody(
                false,
                false,
                Optional.of(StrawCraft.id("civilian")),
                StrawFaction.GOOD
        ));
    }

    @Test
    void ordinaryPassengersCannotSeeScavengerHiddenBodies() {
        assertFalse(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                false,
                Optional.of(StrawCraft.id("civilian")),
                StrawFaction.GOOD
        ));
    }

    @Test
    void killersNeutralsSpectatorsAndScavengerCanSeeHiddenBodies() {
        assertTrue(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                false,
                Optional.of(StrawCraft.id("bomber")),
                StrawFaction.KILLER
        ));
        assertTrue(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                false,
                Optional.of(StrawCraft.id("vulture")),
                StrawFaction.NEUTRAL
        ));
        assertTrue(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                true,
                Optional.empty(),
                StrawFaction.GOOD
        ));
        assertTrue(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                false,
                Optional.of(ScavengerHiddenBodyVisibility.SCAVENGER_ROLE),
                StrawFaction.KILLER
        ));
    }

    @Test
    void unknownPassengerSideRolesStayConservativeAndCannotSeeHiddenBodies() {
        assertFalse(ScavengerHiddenBodyVisibility.canSeeBody(
                true,
                false,
                Optional.of(Identifier.of("other", "passenger_like")),
                StrawFaction.NONE
        ));
    }
}
