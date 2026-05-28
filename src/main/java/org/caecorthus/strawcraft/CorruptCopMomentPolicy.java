package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawKillEvents;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CorruptCopMomentPolicy {
    public static final Identifier CORRUPT_COP_ROLE = StrawCraft.id("corrupt_cop");
    public static final Identifier LAST_STAND_TRIGGER = StrawCraft.id("corrupt_cop_last_stand");
    public static final int MIN_TRIGGER_ALIVE_COUNT = 2;
    public static final int MOMENT_GUN_COOLDOWN_TICKS = 2 * 20;

    private static final String MOMENT_ACTIVE_FLAG = "corrupt_cop.moment_active";
    private static final String TRIGGER_THRESHOLD_COUNTER = "corrupt_cop.trigger_threshold";

    private CorruptCopMomentPolicy() {
    }

    public static int thresholdFor(int totalPlayers) {
        // Source behavior uses floor(totalPlayers / 5); the minimum trigger gate is checked separately.
        // 源行为使用 totalPlayers / 5 向下取整；最小触发人数门槛单独判定。
        return Math.max(0, totalPlayers / 5);
    }

    public static void resetParticipantState(NoellesRoleState state) {
        state.setCounter(TRIGGER_THRESHOLD_COUNTER, 0);
        state.setFlag(MOMENT_ACTIVE_FLAG, false);
        state.clearNeutralWinClaim(CORRUPT_COP_ROLE);
    }

    public static void resetParticipantState(NoellesRoleStateComponent state) {
        state.setCounter(TRIGGER_THRESHOLD_COUNTER, 0);
        state.setFlag(MOMENT_ACTIVE_FLAG, false);
        state.clearNeutralWinClaim(CORRUPT_COP_ROLE);
    }

    public static void resetRoundState(NoellesRoleState state, int totalPlayers) {
        resetParticipantState(state);
        state.setCounter(TRIGGER_THRESHOLD_COUNTER, thresholdFor(totalPlayers));
    }

    public static void resetRoundState(NoellesRoleStateComponent state, int totalPlayers) {
        resetParticipantState(state);
        state.setCounter(TRIGGER_THRESHOLD_COUNTER, thresholdFor(totalPlayers));
    }

    public static boolean checkAndTriggerMoment(NoellesRoleState state, int currentAliveCount) {
        if (!shouldTriggerMoment(state, currentAliveCount)) {
            return false;
        }
        state.setFlag(MOMENT_ACTIVE_FLAG, true);
        return true;
    }

    public static boolean checkAndTriggerMoment(NoellesRoleStateComponent state, int currentAliveCount) {
        if (!shouldTriggerMoment(state, currentAliveCount)) {
            return false;
        }
        state.setFlag(MOMENT_ACTIVE_FLAG, true);
        return true;
    }

    public static boolean shouldTriggerMoment(NoellesRoleState state, int currentAliveCount) {
        return shouldTrigger(isMomentActive(state), triggerThreshold(state), currentAliveCount);
    }

    public static boolean shouldTriggerMoment(NoellesRoleStateComponent state, int currentAliveCount) {
        return shouldTrigger(isMomentActive(state), triggerThreshold(state), currentAliveCount);
    }

    private static boolean shouldTrigger(boolean active, int triggerThreshold, int currentAliveCount) {
        // Keep the source's guard that prevents one-player micro-rounds from starting the moment.
        // 保留源行为中的保护：只剩一人的小局不会启动黑警时刻。
        return !active
                && triggerThreshold >= MIN_TRIGGER_ALIVE_COUNT
                && currentAliveCount >= MIN_TRIGGER_ALIVE_COUNT
                && currentAliveCount <= triggerThreshold;
    }

    public static boolean endMoment(NoellesRoleState state) {
        if (!isMomentActive(state)) {
            return false;
        }
        state.setFlag(MOMENT_ACTIVE_FLAG, false);
        return true;
    }

    public static boolean endMoment(NoellesRoleStateComponent state) {
        if (!isMomentActive(state)) {
            return false;
        }
        state.setFlag(MOMENT_ACTIVE_FLAG, false);
        return true;
    }

    public static boolean isMomentActive(NoellesRoleState state) {
        return state.hasFlag(MOMENT_ACTIVE_FLAG);
    }

    public static boolean isMomentActive(NoellesRoleStateComponent state) {
        return state.hasFlag(MOMENT_ACTIVE_FLAG);
    }

    public static int triggerThreshold(NoellesRoleState state) {
        return state.getCounter(TRIGGER_THRESHOLD_COUNTER);
    }

    public static int triggerThreshold(NoellesRoleStateComponent state) {
        return state.getCounter(TRIGGER_THRESHOLD_COUNTER);
    }

    public static StrawKillEvents.KillDecision beforeKill(NoellesRoleState state, Identifier deathReason) {
        if (isMomentActive(state) && StrawDeathReasons.ASSASSINATED.equals(deathReason)) {
            return StrawKillEvents.KillDecision.cancel();
        }
        return StrawKillEvents.KillDecision.pass();
    }

    public static StrawKillEvents.KillDecision beforeKill(NoellesRoleStateComponent state, Identifier deathReason) {
        if (isMomentActive(state) && StrawDeathReasons.ASSASSINATED.equals(deathReason)) {
            return StrawKillEvents.KillDecision.cancel();
        }
        return StrawKillEvents.KillDecision.pass();
    }

    public static int gunCooldownTicks(NoellesRoleState state) {
        return isMomentActive(state) ? MOMENT_GUN_COOLDOWN_TICKS : -1;
    }

    public static int gunCooldownTicks(NoellesRoleStateComponent state) {
        return isMomentActive(state) ? MOMENT_GUN_COOLDOWN_TICKS : -1;
    }

    public static WinDecision evaluateWin(List<Participant> participants, DefaultWin defaultWin) {
        Objects.requireNonNull(participants, "participants");
        Objects.requireNonNull(defaultWin, "defaultWin");
        List<Participant> livingAssigned = participants.stream()
                .filter(Participant::assigned)
                .filter(Participant::alive)
                .toList();
        boolean livingCorruptCop = livingAssigned.stream().anyMatch(Participant::corruptCop);
        if (!livingCorruptCop) {
            return WinDecision.PASS;
        }
        if (livingAssigned.size() == 1) {
            return WinDecision.NEUTRAL_WIN;
        }
        if (defaultWin == DefaultWin.KILLERS || defaultWin == DefaultWin.PASSENGERS) {
            return WinDecision.BLOCK_DEFAULT;
        }
        return WinDecision.PASS;
    }

    public enum DefaultWin {
        NONE,
        KILLERS,
        PASSENGERS,
        TIME,
        LOOSE_END
    }

    public enum WinDecision {
        PASS,
        BLOCK_DEFAULT,
        NEUTRAL_WIN
    }

    public record Participant(UUID uuid, boolean assigned, boolean alive, boolean corruptCop) {
        public Participant {
            Objects.requireNonNull(uuid, "uuid");
        }
    }
}
