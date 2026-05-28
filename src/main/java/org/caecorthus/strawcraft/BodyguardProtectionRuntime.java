package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.caecorthus.strawcraft.api.StrawKillEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BodyguardProtectionRuntime {
    private BodyguardProtectionRuntime() {
    }

    public static void registerEvents() {
        StrawKillEvents.BEFORE_KILL.register((victim, killer, deathReason) -> {
            if (!(victim instanceof ServerPlayerEntity serverVictim)) {
                return StrawKillEvents.KillDecision.pass();
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
            return BodyguardProtectionPolicy.beforeKill(new BodyguardProtectionPolicy.Input(
                    victim.getUuid(),
                    game.isRunning(),
                    GameFunctions.isPlayerAliveAndSurvival(serverVictim),
                    deathReason,
                    candidates(serverVictim, game)
            ));
        });
    }

    private static List<BodyguardProtectionPolicy.Candidate> candidates(
            ServerPlayerEntity victim,
            GameWorldComponent game
    ) {
        List<BodyguardProtectionPolicy.Candidate> candidates = new ArrayList<>();
        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            PlayerEntity candidate = victim.getServerWorld().getPlayerByUuid(entry.getKey());
            if (!(candidate instanceof ServerPlayerEntity bodyguard)) {
                continue;
            }
            candidates.add(new BodyguardProtectionPolicy.Candidate(
                    entry.getKey(),
                    entry.getValue(),
                    GameFunctions.isPlayerAliveAndSurvival(bodyguard),
                    bodyguard.squaredDistanceTo(victim),
                    BodyguardProtectionPolicy.charge(NoellesRoleStateComponent.KEY.get(bodyguard))
            ));
        }
        return candidates;
    }
}
