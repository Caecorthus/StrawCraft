package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawDeathEvents;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StrawCorpseMetadata {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<UUID, CorpseMetadata> BY_DEAD_PLAYER = new ConcurrentHashMap<>();

    private StrawCorpseMetadata() {
    }

    public static void registerEvents() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.register(StrawCorpseMetadata::recordOfficialDeath);
    }

    public static void recordOfficialDeath(StrawDeathEvents.OfficialDeathContext context) {
        // Wathe owns body creation and spectator switching; StrawCraft only records query metadata.
        // Wathe 负责创建尸体和切换旁观；StrawCraft 这里只保存可查询的附加元数据。
        BY_DEAD_PLAYER.put(context.victimUuid(), new CorpseMetadata(
                context.victimUuid(),
                context.killerUuid(),
                context.indirectAttribution(),
                context.deathReason(),
                context.gameTime(),
                context.spawnBodyRequested(),
                context.watheBaselineOwnsBodyAndSpectator()
        ));
    }

    public static Optional<CorpseMetadata> byDeadPlayer(UUID deadPlayerUuid) {
        return Optional.ofNullable(BY_DEAD_PLAYER.get(deadPlayerUuid));
    }

    public static void clearAll() {
        BY_DEAD_PLAYER.clear();
    }

    public record CorpseMetadata(
            UUID deadPlayerUuid,
            Optional<UUID> killerUuid,
            boolean indirectAttribution,
            Identifier deathReason,
            long gameTime,
            boolean spawnBodyRequested,
            boolean watheBaselineOwnsBodyAndSpectator
    ) {
    }
}
