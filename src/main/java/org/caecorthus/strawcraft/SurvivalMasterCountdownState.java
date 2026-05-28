package org.caecorthus.strawcraft;

public final class SurvivalMasterCountdownState {
    public static final int COUNTDOWN_SECONDS = 120;

    private int remainingSeconds;
    private boolean running;
    private boolean completed;

    public Update tick(SurvivalMasterCountdownPolicy.Observation observation, RoundEndSink roundEndSink) {
        if (completed) {
            return Update.IDLE;
        }
        if (running && !observation.survivalMasterAlive()) {
            running = false;
            remainingSeconds = 0;
            return new Update(false, false, true, false, remainingSeconds);
        }
        if (!running) {
            if (!SurvivalMasterCountdownPolicy.shouldStartCountdown(observation)) {
                return Update.IDLE;
            }
            running = true;
            remainingSeconds = COUNTDOWN_SECONDS;
            return new Update(true, false, false, false, remainingSeconds);
        }

        remainingSeconds--;
        if (remainingSeconds <= 0) {
            running = false;
            completed = true;
            remainingSeconds = 0;
            roundEndSink.endRoundAsPassengers();
            return new Update(false, false, false, true, remainingSeconds);
        }
        return new Update(false, true, false, false, remainingSeconds);
    }

    public void reset() {
        // Countdown state is round-scoped and must not survive Wathe initialize/finalize boundaries.
        // 倒计时状态只属于当前回合，不能跨过 Wathe 初始化/结算边界残留。
        // 倒计时状态只属于当前回合，不能跨过 Wathe 的初始化/收尾边界。
        remainingSeconds = 0;
        running = false;
        completed = false;
    }

    public int remainingSeconds() {
        return remainingSeconds;
    }

    public boolean running() {
        return running;
    }

    public boolean completed() {
        return completed;
    }

    public interface RoundEndSink {
        void endRoundAsPassengers();
    }

    public record Update(
            boolean started,
            boolean progress,
            boolean cancelled,
            boolean completed,
            int remainingSeconds
    ) {
        public static final Update IDLE = new Update(false, false, false, false, 0);
    }
}
