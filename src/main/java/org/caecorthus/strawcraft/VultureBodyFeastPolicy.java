package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

public final class VultureBodyFeastPolicy {
    public static final Identifier VULTURE_ROLE = StrawCraft.id("vulture");
    public static final Identifier BODY_FEAST_TRIGGER = StrawCraft.id("vulture_body_feast");
    public static final String VULTURE_EAT_COOLDOWN = "vulture_body_feast";
    public static final int FEAST_COOLDOWN_TICKS = 5 * 20;
    public static final double FEAST_RANGE = 5.0D;
    public static final double FEAST_RANGE_SQUARED = FEAST_RANGE * FEAST_RANGE;

    private static final String BODIES_EATEN_COUNTER = "vulture.bodies_eaten";
    private static final String BODIES_REQUIRED_COUNTER = "vulture.bodies_required";
    private static final String EATEN_BODIES_SET = "vulture.eaten_bodies";
    private static final String WON_FLAG = "vulture.won";

    private VultureBodyFeastPolicy() {
    }

    public static int bodiesRequiredFor(int totalPlayers) {
        // XruiDD uses floor(totalPlayers / 2); StrawCraft keeps that shape but prevents a zero-body win.
        // XruiDD 使用 floor(totalPlayers / 2)；StrawCraft 保留这个形状，但避免 0 具尸体直接胜利。
        return Math.max(1, totalPlayers / 2);
    }

    public static void resetRoundState(NoellesRoleState state, int totalPlayers) {
        state.setCounter(BODIES_EATEN_COUNTER, 0);
        state.setCounter(BODIES_REQUIRED_COUNTER, bodiesRequiredFor(totalPlayers));
        state.clearUuidSet(EATEN_BODIES_SET);
        state.setFlag(WON_FLAG, false);
        state.clearNeutralWinClaim(VULTURE_ROLE);
    }

    public static void resetRoundState(NoellesRoleStateComponent state, int totalPlayers) {
        state.setCounter(BODIES_EATEN_COUNTER, 0);
        state.setCounter(BODIES_REQUIRED_COUNTER, bodiesRequiredFor(totalPlayers));
        state.clearUuidSet(EATEN_BODIES_SET);
        state.setFlag(WON_FLAG, false);
        state.clearNeutralWinClaim(VULTURE_ROLE);
    }

    public static FeastResult recordSuccessfulFeast(NoellesRoleState state, UUID bodyUuid, long gameTime) {
        if (!state.addUuidToSet(EATEN_BODIES_SET, bodyUuid)) {
            return FeastResult.duplicate(bodiesEaten(state), bodiesRequired(state), hasWon(state));
        }

        int eaten = state.incrementCounter(BODIES_EATEN_COUNTER);
        int required = bodiesRequired(state);
        boolean won = eaten >= required;
        if (won) {
            state.setFlag(WON_FLAG, true);
            state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                    VULTURE_ROLE,
                    BODY_FEAST_TRIGGER,
                    Optional.empty(),
                    gameTime
            ));
        }
        return new FeastResult(true, false, eaten, required, won);
    }

    public static FeastResult recordSuccessfulFeast(NoellesRoleStateComponent state, UUID bodyUuid, long gameTime) {
        if (!state.addUuidToSet(EATEN_BODIES_SET, bodyUuid)) {
            return FeastResult.duplicate(bodiesEaten(state), bodiesRequired(state), hasWon(state));
        }

        int eaten = state.incrementCounter(BODIES_EATEN_COUNTER);
        int required = bodiesRequired(state);
        boolean won = eaten >= required;
        if (won) {
            state.setFlag(WON_FLAG, true);
            state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                    VULTURE_ROLE,
                    BODY_FEAST_TRIGGER,
                    Optional.empty(),
                    gameTime
            ));
        }
        return new FeastResult(true, false, eaten, required, won);
    }

    public static int bodiesEaten(NoellesRoleState state) {
        return state.getCounter(BODIES_EATEN_COUNTER);
    }

    public static int bodiesRequired(NoellesRoleState state) {
        return state.getCounter(BODIES_REQUIRED_COUNTER);
    }

    public static int bodiesEaten(NoellesRoleStateComponent state) {
        return state.getCounter(BODIES_EATEN_COUNTER);
    }

    public static int bodiesRequired(NoellesRoleStateComponent state) {
        return state.getCounter(BODIES_REQUIRED_COUNTER);
    }

    public static boolean hasEatenBody(NoellesRoleState state, UUID bodyUuid) {
        return state.uuidSetContains(EATEN_BODIES_SET, bodyUuid);
    }

    public static boolean hasEatenBody(NoellesRoleStateComponent state, UUID bodyUuid) {
        return state.uuidSetContains(EATEN_BODIES_SET, bodyUuid);
    }

    public static boolean hasWon(NoellesRoleState state) {
        return state.hasFlag(WON_FLAG);
    }

    public static boolean hasWon(NoellesRoleStateComponent state) {
        return state.hasFlag(WON_FLAG);
    }

    public record FeastResult(
            boolean accepted,
            boolean duplicateBody,
            int bodiesEaten,
            int bodiesRequired,
            boolean won
    ) {
        private static FeastResult duplicate(int bodiesEaten, int bodiesRequired, boolean won) {
            return new FeastResult(false, true, bodiesEaten, bodiesRequired, won);
        }
    }
}
