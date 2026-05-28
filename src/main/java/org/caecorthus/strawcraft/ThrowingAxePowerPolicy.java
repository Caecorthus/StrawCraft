package org.caecorthus.strawcraft;

public final class ThrowingAxePowerPolicy {
    public static final int FULL_CHARGE_TICKS = 20;
    public static final float MIN_THROW_POWER = 0.25F;

    private ThrowingAxePowerPolicy() {
    }

    public static Decision evaluateChargeTicks(int chargeTicks) {
        float chargeRatio = Math.max(0, chargeTicks) / (float) FULL_CHARGE_TICKS;
        float power = (chargeRatio * chargeRatio + chargeRatio * 2.0F) / 3.0F;
        if (power > 1.0F) {
            power = 1.0F;
        }
        return new Decision(power >= MIN_THROW_POWER, power);
    }

    public record Decision(boolean accepted, float power) {
    }
}
