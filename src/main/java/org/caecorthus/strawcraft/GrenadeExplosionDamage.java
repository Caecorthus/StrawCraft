package org.caecorthus.strawcraft;

public final class GrenadeExplosionDamage {
    public static final double RADIUS = 3.0;
    public static final float MAX_DAMAGE = 40.0f;
    public static final float MIN_EFFECTIVE_DAMAGE = 1.0f;

    private GrenadeExplosionDamage() {
    }

    public static float damageAt(double distance, float exposure) {
        if (distance >= RADIUS) {
            return 0.0f;
        }
        float impact = (float) ((1.0 - distance / RADIUS) * exposure);
        // Matches vanilla's explosion curve, scaled so an unblocked center hit is 40 damage.
        float damage = MAX_DAMAGE * ((impact * impact + impact) / 2.0f);
        return damage >= MIN_EFFECTIVE_DAMAGE ? damage : 0.0f;
    }
}
