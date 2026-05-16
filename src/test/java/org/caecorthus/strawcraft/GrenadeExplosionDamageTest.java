package org.caecorthus.strawcraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrenadeExplosionDamageTest {
    @Test
    void directUnblockedBlastDealsFullGrenadeDamage() {
        assertEquals(40.0f, GrenadeExplosionDamage.damageAt(0.0, 1.0f));
    }

    @Test
    void blastDoesNotReachOutsideGrenadeRadius() {
        assertEquals(0.0f, GrenadeExplosionDamage.damageAt(3.01, 1.0f));
    }

    @Test
    void exposureReducesDamageThroughVanillaExplosionCurve() {
        assertEquals(15.0f, GrenadeExplosionDamage.damageAt(0.0, 0.5f));
    }

    @Test
    void tinyEdgeDamageIsIgnored() {
        assertEquals(0.0f, GrenadeExplosionDamage.damageAt(2.98, 1.0f));
    }

    @Test
    void fullyBlockedBlastDealsNoDamage() {
        assertEquals(0.0f, GrenadeExplosionDamage.damageAt(0.0, 0.0f));
    }
}
