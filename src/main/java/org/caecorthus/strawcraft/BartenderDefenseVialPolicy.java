package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;

public final class BartenderDefenseVialPolicy {
    private BartenderDefenseVialPolicy() {
    }

    public static DefenseTarget selectDefenseTarget(Input input) {
        if (!StrawRoleMeaning.usesBartenderShop(input.role())
                || !input.carrierAlive()
                || !input.selfPoisoned()) {
            return DefenseTarget.NONE;
        }
        return DefenseTarget.SELF;
    }

    public record Input(Role role, boolean carrierAlive, boolean selfPoisoned) {
    }

    public enum DefenseTarget {
        NONE,
        SELF
    }
}
