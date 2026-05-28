package org.caecorthus.strawcraft;

import org.caecorthus.strawcraft.api.StrawDeathEvents;

public final class NoellesNeutralWinAdapter {
    private NoellesNeutralWinAdapter() {
    }

    public static void registerEvents() {
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(NoellesNeutralWinAdapter::recordRoleDeath);
    }

    static void recordRoleDeath(StrawDeathEvents.RoleDeathContext context) {
        var victim = context.world().getPlayerByUuid(context.official().victimUuid());
        if (victim == null) {
            return;
        }
        NoellesRoleStateComponent state = NoellesRoleStateComponent.KEY.get(victim);
        NoellesNeutralWinPolicy.neutralWinClaimForOfficialDeath(context.victimRoleId(), context.official())
                .ifPresent(state::recordNeutralWinClaim);
    }
}
