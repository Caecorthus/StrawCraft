package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawKillEvents;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

public final class BodyguardProtectionPolicy {
    public static final Identifier BODYGUARD_ROLE = StrawCraft.id("bodyguard");
    public static final String PROTECTION_FLAG = "bodyguard_protection_available";
    public static final String GRANTED_AT_TIMESTAMP = "bodyguard_protection_granted_at";
    public static final double PROTECTION_RANGE = 6.0;
    public static final double PROTECTION_RANGE_SQUARED = PROTECTION_RANGE * PROTECTION_RANGE;

    private BodyguardProtectionPolicy() {
    }

    public static void grant(NoellesRoleState state, long currentGameTime) {
        grant(charge(state), currentGameTime);
    }

    public static void grant(NoellesRoleStateComponent state, long currentGameTime) {
        grant(charge(state), currentGameTime);
    }

    public static StrawKillEvents.KillDecision beforeKill(Input input) {
        if (!input.gameRunning()
                || !input.victimAlive()
                || isExcludedDeathReason(input.deathReason())) {
            return StrawKillEvents.KillDecision.pass();
        }

        return input.candidates().stream()
                .filter(candidate -> isEligible(input.victimUuid(), candidate))
                .min(Comparator
                        .comparingDouble(Candidate::squaredDistanceToVictim)
                        .thenComparing(Candidate::playerUuid))
                .map(BodyguardProtectionPolicy::consume)
                .orElseGet(StrawKillEvents.KillDecision::pass);
    }

    static ProtectionCharge charge(NoellesRoleState state) {
        return new ProtectionCharge() {
            @Override
            public boolean hasFlag(String flag) {
                return state.hasFlag(flag);
            }

            @Override
            public void setFlag(String flag, boolean value) {
                state.setFlag(flag, value);
            }

            @Override
            public OptionalLong getTimestamp(String key) {
                return state.getTimestamp(key);
            }

            @Override
            public void setTimestamp(String key, long tick) {
                state.setTimestamp(key, tick);
            }
        };
    }

    static ProtectionCharge charge(NoellesRoleStateComponent state) {
        return new ProtectionCharge() {
            @Override
            public boolean hasFlag(String flag) {
                return state.hasFlag(flag);
            }

            @Override
            public void setFlag(String flag, boolean value) {
                state.setFlag(flag, value);
            }

            @Override
            public OptionalLong getTimestamp(String key) {
                return state.getTimestamp(key);
            }

            @Override
            public void setTimestamp(String key, long tick) {
                state.setTimestamp(key, tick);
            }
        };
    }

    private static void grant(ProtectionCharge charge, long currentGameTime) {
        if (charge.getTimestamp(GRANTED_AT_TIMESTAMP).isPresent()) {
            return;
        }
        // One round grant mirrors Professor; NoellesRoleState reset clears it between games.
        // 单局只发一次，与 Professor 保持一致；NoellesRoleState reset 会在下一局清掉它。
        charge.setFlag(PROTECTION_FLAG, true);
        charge.setTimestamp(GRANTED_AT_TIMESTAMP, currentGameTime);
    }

    private static boolean isEligible(UUID victimUuid, Candidate candidate) {
        return !victimUuid.equals(candidate.playerUuid())
                && StrawRoleMeaning.receivesBodyguardProtection(candidate.role())
                && candidate.alive()
                && candidate.squaredDistanceToVictim() <= PROTECTION_RANGE_SQUARED
                && candidate.charge().hasFlag(PROTECTION_FLAG);
    }

    private static StrawKillEvents.KillDecision consume(Candidate candidate) {
        // Consume the Bodyguard charge before official Wathe creates the prevented death.
        // 在官方 Wathe 生成被阻止的死亡之前消耗 Bodyguard 保护次数。
        candidate.charge().setFlag(PROTECTION_FLAG, false);
        return StrawKillEvents.KillDecision.cancel();
    }

    private static boolean isExcludedDeathReason(Identifier deathReason) {
        // Excluded deaths keep the Bodyguard charge for the next ordinary Wathe kill.
        // 被排除的死亡原因不消耗 Bodyguard 次数，留给下一次普通 Wathe 击杀。
        return StrawDeathReasons.SHOT_INNOCENT.equals(deathReason)
                || StrawDeathReasons.ASSASSINATED.equals(deathReason);
    }

    interface ProtectionCharge {
        boolean hasFlag(String flag);

        void setFlag(String flag, boolean value);

        OptionalLong getTimestamp(String key);

        void setTimestamp(String key, long tick);
    }

    public record Input(
            UUID victimUuid,
            boolean gameRunning,
            boolean victimAlive,
            Identifier deathReason,
            List<Candidate> candidates
    ) {
        public Input {
            candidates = List.copyOf(candidates);
        }
    }

    public record Candidate(
            UUID playerUuid,
            Role role,
            boolean alive,
            double squaredDistanceToVictim,
            ProtectionCharge charge
    ) {
    }
}
