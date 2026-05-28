package org.caecorthus.strawcraft;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.caecorthus.strawcraft.api.StrawWinEvents;

public final class NoellesNeutralWinAdapter {
    private NoellesNeutralWinAdapter() {
    }

    public static void registerEvents() {
        PayloadTypeRegistry.playS2C().register(NoellesNeutralWinResultPayload.ID, NoellesNeutralWinResultPayload.CODEC);
        StrawDeathEvents.ROLE_DEATH_COMPLETED.register(NoellesNeutralWinAdapter::recordRoleDeath);
        StrawWinEvents.COLLECT_WIN_CONTRIBUTIONS.register(NoellesNeutralWinAdapter::collectRecordedNeutralWins);
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

    private static void collectRecordedNeutralWins(
            StrawWinEvents.WinContext context,
            StrawWinEvents.WinContribution.Builder contribution
    ) {
        if (context.world().isEmpty()) {
            return;
        }
        var world = context.world().orElseThrow();
        for (StrawWinEvents.Participant participant : context.participants()) {
            var player = world.getPlayerByUuid(participant.playerUuid());
            if (player == null) {
                continue;
            }
            NoellesNeutralWinPolicy.contributeRecordedNeutralWins(
                    participant.playerUuid(),
                    NoellesRoleStateComponent.KEY.get(player).neutralWinClaims(),
                    contribution
            );
        }
    }
}
