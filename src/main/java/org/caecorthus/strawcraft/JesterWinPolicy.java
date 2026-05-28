package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.caecorthus.strawcraft.api.StrawWinEvents;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JesterWinPolicy {
    public static final Identifier JESTER_ROLE = StrawCraft.id("jester");
    public static final Identifier TARGET_KILLED_TRIGGER = StrawCraft.id("jester_target_killed");
    public static final Identifier SPARK_ESCAPED_DEATH = Identifier.of("wathe", "escaped");
    public static final int STASIS_TICKS = 5 * 20;
    public static final int PSYCHO_TICKS = 3 * 60 * 20;

    private JesterWinPolicy() {
    }

    public static KillAttemptDecision evaluateKillAttempt(KillAttemptInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.victimJester()) {
            return KillAttemptDecision.pass();
        }
        if (input.jesterInStasis()) {
            if (isStasisResetDeath(input.deathReason())) {
                return KillAttemptDecision.resetStasisAndAllowDeath();
            }
            return KillAttemptDecision.cancelStasisDeath();
        }
        if (input.deathReason().equals(GameConstants.DeathReasons.GUN)
                && input.killerUuid().isPresent()
                && input.killerInnocent()
                && !input.jesterInPsychoMode()) {
            // EN: This is the official Spark trigger for the Jester moment.
            // ZH: 这是 Spark 小丑时刻的正式触发条件。
            return KillAttemptDecision.enterStasis(input.killerUuid().orElseThrow(), STASIS_TICKS);
        }
        return KillAttemptDecision.pass();
    }

    public static boolean targetDeathCompletesWin(Optional<UUID> targetKiller, UUID deadPlayer) {
        Objects.requireNonNull(targetKiller, "targetKiller");
        Objects.requireNonNull(deadPlayer, "deadPlayer");
        return targetKiller.filter(deadPlayer::equals).isPresent();
    }

    public static WinCheckDecision evaluateWinCheck(WinCheckInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.jesterAlive()) {
            return WinCheckDecision.PASS;
        }
        if (input.jesterWon()) {
            return WinCheckDecision.NEUTRAL_WIN;
        }
        if (input.jesterInPsychoMode()
                && (input.defaultWin() == StrawWinEvents.DefaultWin.KILLERS
                || input.defaultWin() == StrawWinEvents.DefaultWin.PASSENGERS)) {
            return WinCheckDecision.BLOCK_DEFAULT_WIN;
        }
        return WinCheckDecision.PASS;
    }

    public static void resetParticipantState(NoellesRoleState state) {
        state.clearNeutralWinClaim(JESTER_ROLE);
        state.clearJesterMomentState();
    }

    public static void resetParticipantState(NoellesRoleStateComponent state) {
        state.clearNeutralWinClaim(JESTER_ROLE);
        state.clearJesterMomentState();
    }

    private static boolean isStasisResetDeath(Identifier deathReason) {
        return deathReason.equals(GameConstants.DeathReasons.FELL_OUT_OF_TRAIN)
                || deathReason.equals(SPARK_ESCAPED_DEATH);
    }

    public enum KillAttemptAction {
        PASS,
        ENTER_STASIS,
        CANCEL_STASIS_DEATH,
        RESET_STASIS_AND_ALLOW_DEATH
    }

    public enum WinCheckDecision {
        PASS,
        NEUTRAL_WIN,
        BLOCK_DEFAULT_WIN
    }

    public record KillAttemptInput(
            boolean victimJester,
            boolean jesterInStasis,
            boolean jesterInPsychoMode,
            Optional<UUID> killerUuid,
            boolean killerInnocent,
            Identifier deathReason
    ) {
        public KillAttemptInput {
            Objects.requireNonNull(killerUuid, "killerUuid");
            Objects.requireNonNull(deathReason, "deathReason");
        }
    }

    public record KillAttemptDecision(
            KillAttemptAction action,
            boolean cancelWatheKill,
            Optional<UUID> targetKiller,
            int stasisTicks
    ) {
        public KillAttemptDecision {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(targetKiller, "targetKiller");
        }

        static KillAttemptDecision pass() {
            return new KillAttemptDecision(KillAttemptAction.PASS, false, Optional.empty(), 0);
        }

        static KillAttemptDecision enterStasis(UUID targetKiller, int stasisTicks) {
            return new KillAttemptDecision(KillAttemptAction.ENTER_STASIS, true, Optional.of(targetKiller), stasisTicks);
        }

        static KillAttemptDecision cancelStasisDeath() {
            return new KillAttemptDecision(KillAttemptAction.CANCEL_STASIS_DEATH, true, Optional.empty(), 0);
        }

        static KillAttemptDecision resetStasisAndAllowDeath() {
            return new KillAttemptDecision(KillAttemptAction.RESET_STASIS_AND_ALLOW_DEATH, false, Optional.empty(), 0);
        }
    }

    public record WinCheckInput(
            boolean jesterAlive,
            boolean jesterWon,
            boolean jesterInPsychoMode,
            StrawWinEvents.DefaultWin defaultWin
    ) {
        public WinCheckInput {
            Objects.requireNonNull(defaultWin, "defaultWin");
        }
    }
}
