package org.caecorthus.strawcraft;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponBalanceTest {
    @Test
    void attackerDirectlyBehindTargetCountsAsBackstab() {
        assertTrue(WeaponBalance.isInsideBackstabArc(new Vec3d(0.0, 0.0, 1.0), new Vec3d(0.0, 0.0, -1.0)));
    }

    @Test
    void attackerAtSixtyDegreesFromBackStillCountsAsBackstab() {
        assertTrue(WeaponBalance.isInsideBackstabArc(new Vec3d(0.0, 0.0, 1.0), new Vec3d(Math.sin(Math.toRadians(60.0)), 0.0, -Math.cos(Math.toRadians(60.0)))));
    }

    @Test
    void attackerOutsideBackArcDoesNotCountAsBackstab() {
        assertFalse(WeaponBalance.isInsideBackstabArc(new Vec3d(0.0, 0.0, 1.0), new Vec3d(Math.sin(Math.toRadians(70.0)), 0.0, -Math.cos(Math.toRadians(70.0)))));
    }

    @Test
    void attackerInFrontDoesNotCountAsBackstab() {
        assertFalse(WeaponBalance.isInsideBackstabArc(new Vec3d(0.0, 0.0, 1.0), new Vec3d(0.0, 0.0, 1.0)));
    }
}
