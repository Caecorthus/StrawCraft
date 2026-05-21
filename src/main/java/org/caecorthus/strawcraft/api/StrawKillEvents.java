package org.caecorthus.strawcraft.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class StrawKillEvents {
    public static final Event<BeforeKill> BEFORE_KILL = EventFactory.createArrayBacked(
            BeforeKill.class,
            listeners -> (victim, killer, deathReason) -> {
                for (BeforeKill listener : listeners) {
                    KillDecision decision = listener.beforeKill(victim, killer, deathReason);
                    if (decision.cancelWatheKill()) {
                        return decision;
                    }
                }
                return KillDecision.pass();
            }
    );

    private StrawKillEvents() {
    }

    public interface BeforeKill {
        KillDecision beforeKill(PlayerEntity victim, @Nullable PlayerEntity killer, Identifier deathReason);
    }

    public record KillDecision(boolean cancelWatheKill) {
        public static KillDecision pass() {
            return new KillDecision(false);
        }

        public static KillDecision cancel() {
            return new KillDecision(true);
        }
    }
}
