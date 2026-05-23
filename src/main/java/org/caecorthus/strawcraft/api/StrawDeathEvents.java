package org.caecorthus.strawcraft.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class StrawDeathEvents {
    public static final Event<OfficialDeathCompleted> OFFICIAL_DEATH_COMPLETED = EventFactory.createArrayBacked(
            OfficialDeathCompleted.class,
            listeners -> context -> {
                for (OfficialDeathCompleted listener : listeners) {
                    listener.onOfficialDeathCompleted(context);
                }
            }
    );

    private StrawDeathEvents() {
    }

    public interface OfficialDeathCompleted {
        void onOfficialDeathCompleted(OfficialDeathContext context);
    }

    public record OfficialDeathContext(
            UUID victimUuid,
            Optional<UUID> killerUuid,
            boolean indirectAttribution,
            Identifier deathReason,
            long gameTime,
            boolean spawnBodyRequested,
            boolean watheBaselineOwnsBodyAndSpectator
    ) {
        public OfficialDeathContext {
            Objects.requireNonNull(victimUuid, "victimUuid");
            Objects.requireNonNull(killerUuid, "killerUuid");
            Objects.requireNonNull(deathReason, "deathReason");
        }
    }
}
