package org.caecorthus.strawcraft.map;

import java.util.random.RandomGenerator;

public final class WeightedVotePicker {
    private WeightedVotePicker() {
    }

    public static int pick(int optionCount, int[] voteCounts, RandomGenerator random) {
        if (optionCount <= 0) {
            return -1;
        }

        boolean hasVotes = false;
        for (int i = 0; i < optionCount && i < voteCounts.length; i++) {
            if (voteCounts[i] > 0) {
                hasVotes = true;
                break;
            }
        }

        int totalWeight = 0;
        int[] weights = new int[optionCount];
        for (int i = 0; i < optionCount; i++) {
            // With no votes, every option should still be eligible instead of defaulting to index 0.
            // 没有人投票时，每个选项仍然应当可被抽中，而不是默认落到第 0 项。
            weights[i] = hasVotes ? voteCountAt(voteCounts, i) : 1;
            totalWeight += weights[i];
        }
        if (totalWeight <= 0) {
            return 0;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return i;
            }
        }
        return 0;
    }

    private static int voteCountAt(int[] voteCounts, int index) {
        return index < voteCounts.length ? Math.max(0, voteCounts[index]) : 0;
    }
}
