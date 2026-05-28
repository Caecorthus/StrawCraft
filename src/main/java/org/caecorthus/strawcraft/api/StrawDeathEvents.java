package org.caecorthus.strawcraft.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class StrawDeathEvents {
    public static final Identifier UNKNOWN_WORLD = Identifier.of("strawcraft", "unknown_world");

    public static final Event<OfficialDeathCompleted> OFFICIAL_DEATH_COMPLETED = EventFactory.createArrayBacked(
            OfficialDeathCompleted.class,
            listeners -> context -> {
                for (OfficialDeathCompleted listener : listeners) {
                    listener.onOfficialDeathCompleted(context);
                }
            }
    );
    public static final Event<RoleDeathCompleted> ROLE_DEATH_COMPLETED = EventFactory.createArrayBacked(
            RoleDeathCompleted.class,
            listeners -> context -> {
                for (RoleDeathCompleted listener : listeners) {
                    listener.onRoleDeathCompleted(context);
                }
            }
    );

    private StrawDeathEvents() {
    }

    public interface OfficialDeathCompleted {
        void onOfficialDeathCompleted(OfficialDeathContext context);
    }

    public interface RoleDeathCompleted {
        void onRoleDeathCompleted(RoleDeathContext context);
    }

    public record OfficialDeathContext(
            Identifier worldId,
            UUID victimUuid,
            Optional<UUID> killerUuid,
            boolean indirectAttribution,
            Identifier deathReason,
            long gameTime,
            boolean spawnBodyRequested,
            boolean watheBaselineOwnsBodyAndSpectator
    ) {
        public OfficialDeathContext {
            Objects.requireNonNull(worldId, "worldId");
            Objects.requireNonNull(victimUuid, "victimUuid");
            Objects.requireNonNull(killerUuid, "killerUuid");
            Objects.requireNonNull(deathReason, "deathReason");
        }

        public OfficialDeathContext(
                UUID victimUuid,
                Optional<UUID> killerUuid,
                boolean indirectAttribution,
                Identifier deathReason,
                long gameTime,
                boolean spawnBodyRequested,
                boolean watheBaselineOwnsBodyAndSpectator
        ) {
            this(
                    UNKNOWN_WORLD,
                    victimUuid,
                    killerUuid,
                    indirectAttribution,
                    deathReason,
                    gameTime,
                    spawnBodyRequested,
                    watheBaselineOwnsBodyAndSpectator
            );
        }
    }

    public record RoleDeathContext(
            ServerWorld world,
            OfficialDeathContext official,
            Optional<Identifier> victimRoleId,
            Optional<Identifier> killerRoleId
    ) {
        public RoleDeathContext {
            // Role-aware death events let adapters react without importing Spark KillPlayer.AFTER.
            // 带职业信息的死亡事件让适配器可以响应死亡，不需要导入 Spark 的 KillPlayer.AFTER。
            Objects.requireNonNull(world, "world");
            Objects.requireNonNull(official, "official");
            Objects.requireNonNull(victimRoleId, "victimRoleId");
            Objects.requireNonNull(killerRoleId, "killerRoleId");
        }
    }
}
