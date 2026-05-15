package org.caecorthus.strawcraft;

public enum GunAmmoFaction {
    POLICE(30 * 20),
    CIVILIAN(45 * 20),
    KILLER(60 * 20);

    private final int refillDelayTicks;

    GunAmmoFaction(int refillDelayTicks) {
        this.refillDelayTicks = refillDelayTicks;
    }

    public int refillDelayTicks() {
        return refillDelayTicks;
    }
}
