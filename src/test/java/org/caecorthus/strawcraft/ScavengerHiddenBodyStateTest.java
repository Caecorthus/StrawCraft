package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScavengerHiddenBodyStateTest {
    @Test
    void recordsHiddenBodiesPerWorldAndDoesNotLeakAcrossWorlds() {
        ScavengerHiddenBodyState state = new ScavengerHiddenBodyState();
        Identifier overworld = Identifier.of("minecraft", "overworld");
        Identifier nether = Identifier.of("minecraft", "the_nether");
        UUID victim = new UUID(0, 1);

        state.recordHiddenBody(overworld, victim);

        assertTrue(state.isHiddenBody(overworld, victim));
        assertFalse(state.isHiddenBody(nether, victim));
    }

    @Test
    void resetClearsRoundScopedHiddenBodies() {
        ScavengerHiddenBodyState state = new ScavengerHiddenBodyState();
        Identifier world = Identifier.of("minecraft", "overworld");
        UUID victim = new UUID(0, 1);

        state.recordHiddenBody(world, victim);
        state.clearWorld(world);

        assertFalse(state.isHiddenBody(world, victim));
    }

    @Test
    void hiddenBodyMarkingIsLimitedToScavengerOwnedWatheBodyKills() {
        Optional<Identifier> scavenger = Optional.of(StrawCraft.id("scavenger"));
        Optional<Identifier> ordinaryKiller = Optional.of(WatheRoleIds.KILLER);

        assertTrue(ScavengerHiddenBodies.shouldRecordHiddenBody(true, true, scavenger));
        assertFalse(ScavengerHiddenBodies.shouldRecordHiddenBody(true, true, ordinaryKiller));
        assertFalse(ScavengerHiddenBodies.shouldRecordHiddenBody(false, true, scavenger));
        assertFalse(ScavengerHiddenBodies.shouldRecordHiddenBody(true, false, scavenger));
        assertFalse(ScavengerHiddenBodies.shouldRecordHiddenBody(true, true, Optional.empty()));
    }

    @Test
    void spawnedBodyMarkingKeepsClientAndOrdinaryKillerFlowUnchanged() {
        Optional<Identifier> scavenger = Optional.of(StrawCraft.id("scavenger"));
        Optional<Identifier> ordinaryKiller = Optional.of(WatheRoleIds.KILLER);

        assertTrue(ScavengerHiddenBodies.shouldMarkSpawnedBody(false, scavenger));
        assertFalse(ScavengerHiddenBodies.shouldMarkSpawnedBody(true, scavenger));
        assertFalse(ScavengerHiddenBodies.shouldMarkSpawnedBody(false, ordinaryKiller));
        assertFalse(ScavengerHiddenBodies.shouldMarkSpawnedBody(false, Optional.empty()));
    }
}
