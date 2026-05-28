package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DetectiveKillHistory {
    public static final int DEFAULT_LOOKBACK_TICKS = 2 * 60 * 20;

    // Noelles Detective ignores poison, bomb, and assassination kills; StrawCraft owns migrated Noelles reason ids.
    // Noelles 侦探会忽略毒杀、炸弹和刺客猜杀；迁移后这些 Noelles 死因 id 由 StrawCraft 自己持有。
    private static final Set<Identifier> IMMUNE_DEATH_REASONS = Set.of(
            GameConstants.DeathReasons.POISON,
            StrawDeathReasons.BOMB,
            StrawDeathReasons.ASSASSINATED
    );

    private final List<KillRecord> records = new ArrayList<>();

    public void recordKill(@Nullable UUID killerUuid, @Nullable UUID victimUuid, @Nullable Identifier deathReason, long gameTime) {
        if (killerUuid == null || victimUuid == null || deathReason == null) {
            return;
        }
        records.add(new KillRecord(killerUuid, victimUuid, deathReason, gameTime));
    }

    public boolean hasRecentNonImmuneKill(@Nullable UUID playerUuid, long currentGameTime) {
        return hasRecentNonImmuneKill(playerUuid, DEFAULT_LOOKBACK_TICKS, currentGameTime);
    }

    public boolean hasRecentNonImmuneKill(@Nullable UUID playerUuid, int lookbackTicks, long currentGameTime) {
        if (playerUuid == null || lookbackTicks < 0) {
            return false;
        }
        for (KillRecord record : records) {
            long age = currentGameTime - record.gameTime();
            if (record.killerUuid().equals(playerUuid)
                    && age >= 0
                    && age <= lookbackTicks
                    && !isImmuneDeathReason(record.deathReason())) {
                return true;
            }
        }
        return false;
    }

    public void expireOldKills(long currentGameTime) {
        records.removeIf(record -> currentGameTime - record.gameTime() > DEFAULT_LOOKBACK_TICKS);
    }

    public void reset() {
        records.clear();
    }

    public static boolean isImmuneDeathReason(@Nullable Identifier deathReason) {
        return deathReason != null && IMMUNE_DEATH_REASONS.contains(deathReason);
    }

    private record KillRecord(UUID killerUuid, UUID victimUuid, Identifier deathReason, long gameTime) {
    }
}
