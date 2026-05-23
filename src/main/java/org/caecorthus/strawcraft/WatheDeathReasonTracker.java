package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.Entity;
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
    // 这些死因只是给 Wathe 原版死亡兜底逻辑用的短生命周期线索。
    private static final Map<UUID, DeathAttribution> RECENT_DEATH_ATTRIBUTIONS = new ConcurrentHashMap<>();
    private static final Set<Identifier> TACZ_BULLET_DAMAGE_TYPES = Set.of(
            Identifier.of("tacz", "bullet"),
            Identifier.of("tacz", "bullet_ignore_armor"),
            Identifier.of("tacz", "bullet_void"),
            Identifier.of("tacz", "bullet_void_ignore_armor")
    );

    private WatheDeathReasonTracker() {
    }

    public static void rememberDeathAttribution(UUID victimUuid, Identifier deathReason, UUID killerUuid) {
        rememberDeathAttribution(victimUuid, deathReason, killerUuid, false);
    }

    public static void rememberDeathAttribution(UUID victimUuid, Identifier deathReason, UUID killerUuid, boolean indirect) {
        RECENT_DEATH_ATTRIBUTIONS.put(victimUuid, new DeathAttribution(deathReason, Optional.ofNullable(killerUuid), indirect));
    }

    public static void rememberIndirectDeathAttribution(UUID victimUuid, Identifier deathReason, UUID killerUuid) {
        // Indirect sources such as poison can point Wathe at the rewarded killer without copying Wathe code.
        // 毒药等间接来源可以把 Wathe 指向应获主奖励的杀手，而不需要复制 Wathe 源码。
        rememberDeathAttribution(victimUuid, deathReason, killerUuid, true);
    }

    public static void rememberShotInnocentDeath(UUID victimUuid) {
        rememberDeathAttribution(victimUuid, StrawDeathReasons.SHOT_INNOCENT, (UUID) null);
    }

    public static void rememberDeathAttribution(UUID victimUuid, Identifier deathReason, DamageSource source) {
        rememberDeathAttribution(victimUuid, deathReason, serverPlayerAttackerUuid(source).orElse(null));
    }

    public static void rememberDeathReason(UUID victimUuid, Identifier deathReason) {
        rememberDeathAttribution(victimUuid, deathReason, (UUID) null);
    }

    public static void clearDeathReason(UUID victimUuid) {
        RECENT_DEATH_ATTRIBUTIONS.remove(victimUuid);
    }

    public static Identifier consumeDeathReason(UUID victimUuid, Identifier fallbackDeathReason) {
        return consumeDeathAttribution(victimUuid, fallbackDeathReason)
                .map(DeathAttribution::deathReason)
                .orElse(fallbackDeathReason);
    }

    public static Optional<DeathAttribution> consumeDeathAttribution(UUID victimUuid, Identifier fallbackDeathReason) {
        DeathAttribution attribution = RECENT_DEATH_ATTRIBUTIONS.remove(victimUuid);
        if (attribution != null) {
            return Optional.of(attribution);
        }
        return Optional.of(new DeathAttribution(fallbackDeathReason, Optional.empty(), false));
    }

    public static Optional<Identifier> watheReasonForDamageType(Identifier damageTypeId) {
        if (TACZ_BULLET_DAMAGE_TYPES.contains(damageTypeId)) {
            return Optional.of(GameConstants.DeathReasons.GUN);
        }
        return Optional.empty();
    }

    public static boolean damageWithReason(ServerPlayerEntity victim, Identifier deathReason, DamageSource source, float amount) {
        rememberDeathAttribution(victim.getUuid(), deathReason, source);
        boolean damaged = victim.damage(source, amount);
        if (victim.isAlive()) {
            // Non-lethal hits must not leak their reason into a later unrelated death.
            // 非致命伤害不能把自己的死因泄漏到之后无关的死亡里。
            clearDeathReason(victim.getUuid());
        }
        return damaged;
    }

    private static Optional<UUID> serverPlayerAttackerUuid(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity serverPlayer) {
            return Optional.of(serverPlayer.getUuid());
        }
        return Optional.empty();
    }

    public record DeathAttribution(Identifier deathReason, Optional<UUID> killerUuid, boolean indirect) {
    }
}
