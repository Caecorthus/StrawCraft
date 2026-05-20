package org.caecorthus.strawcraft;

import org.caecorthus.strawcraft.map.WeightedVotePicker;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightedVotePickerTest {
    @Test
    void noVotesGiveEveryOptionEqualWeight() {
        assertEquals(2, WeightedVotePicker.pick(3, new int[]{0, 0, 0}, fixedRoll(2)));
    }

    @Test
    void votesBecomeSelectionWeight() {
        assertEquals(1, WeightedVotePicker.pick(3, new int[]{0, 2, 1}, fixedRoll(1)));
        assertEquals(2, WeightedVotePicker.pick(3, new int[]{0, 2, 1}, fixedRoll(2)));
    }

    @Test
    void emptyOptionsCannotBeSelected() {
        assertEquals(-1, WeightedVotePicker.pick(0, new int[0], fixedRoll(0)));
    }

    private static RandomGenerator fixedRoll(int value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return Math.floorMod(value, bound);
            }
        };
    }
}
