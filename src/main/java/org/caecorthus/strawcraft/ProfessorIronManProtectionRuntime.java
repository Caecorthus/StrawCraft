package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.caecorthus.strawcraft.api.StrawKillEvents;

public final class ProfessorIronManProtectionRuntime {
    private ProfessorIronManProtectionRuntime() {
    }

    public static void registerEvents() {
        StrawKillEvents.BEFORE_KILL.register((victim, killer, deathReason) ->
                ProfessorIronManProtection.beforeKill(
                        GameWorldComponent.KEY.get(victim.getWorld()).getRoles().get(victim.getUuid()),
                        NoellesRoleStateComponent.KEY.get(victim),
                        deathReason
                ));
    }
}
