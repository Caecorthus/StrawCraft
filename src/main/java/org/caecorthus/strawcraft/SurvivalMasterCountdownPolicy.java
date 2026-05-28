package org.caecorthus.strawcraft;

public final class SurvivalMasterCountdownPolicy {
    private SurvivalMasterCountdownPolicy() {
    }

    public static int thresholdForStartingKillers(int startingKillerCount) {
        if (startingKillerCount <= 0) {
            return 0;
        }
        return Math.min(startingKillerCount + 1, 4);
    }

    public static boolean shouldStartCountdown(Observation observation) {
        int threshold = thresholdForStartingKillers(observation.startingKillerCount());
        return threshold > 0
                && observation.survivalMasterAlive()
                && observation.livingPlayerCount() <= threshold;
    }

    public record Observation(
            int startingKillerCount,
            int livingPlayerCount,
            boolean survivalMasterAlive
    ) {
    }
}
