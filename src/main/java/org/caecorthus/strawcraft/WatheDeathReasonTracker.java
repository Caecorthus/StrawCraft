package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WatheDeathReasonTracker {
    // These reasons are short-lived breadcrumbs for Wathe's vanilla-death safety net.
    private static final Map<UUID, Identifier> RECENT_DEATH_REASONS = new ConcurrentHashMap<>();
    private static final Set<Identifier> TACZ_BULLET_DAMAGE_TYPES = Set.of(
            Identifier.of("tacz", "bullet"),
            Identifier.of("tacz", "bullet_ignore_armor"),
            Identifier.of("tacz", "bullet_void"),
            Identifier.of("tacz", "bullet_void_ignore_armor")
    );

    private WatheDeathReasonTracker() {
    }

    public static void rememberDeathReason(UUID victimUuid, Identifier deathReason) {
        RECENT_DEATH_REASONS.put(victimUuid, deathReason);
    }

    public static void clearDeathReason(UUID victimUuid) {
        RECENT_DEATH_REASONS.remove(victimUuid);
    }

    public static Identifier consumeDeathReason(UUID victimUuid, Identifier fallbackDeathReason) {
        Identifier trackedReason = RECENT_DEATH_REASONS.remove(victimUuid);
        return trackedReason == null ? fallbackDeathReason : trackedReason;
    }

    public static Optional<Identifier> watheReasonForDamageType(Identifier damageTypeId) {
        if (TACZ_BULLET_DAMAGE_TYPES.contains(damageTypeId)) {
            return Optional.of(GameConstants.DeathReasons.GUN);
        }
        return Optional.empty();
    }

    public static boolean damageWithReason(ServerPlayerEntity victim, Identifier deathReason, DamageSource source, float amount) {
        rememberDeathReason(victim.getUuid(), deathReason);
        boolean damaged = victim.damage(source, amount);
        if (victim.isAlive()) {
            // Non-lethal hits must not leak their reason into a later unrelated death.
            clearDeathReason(victim.getUuid());
        }
        return damaged;
    }
}
