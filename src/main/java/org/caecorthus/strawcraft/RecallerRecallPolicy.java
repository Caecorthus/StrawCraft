package org.caecorthus.strawcraft;

public final class RecallerRecallPolicy {
    public static final String ABILITY_ID = "recaller_recall";
    public static final int RECALL_PRICE = 100;
    public static final int STORE_COOLDOWN_TICKS = 10 * 20;
    public static final int RECALL_COOLDOWN_TICKS = 30 * 20;

    private RecallerRecallPolicy() {
    }

    public static Result validateActivation(Input input) {
        if (!input.roundRunning()) {
            return Result.NOT_RUNNING;
        }
        if (!input.recallerRole()) {
            return Result.NOT_RECALLER;
        }
        if (!input.playerAlive()) {
            return Result.NOT_ALIVE;
        }
        if (!input.cooldownReady()) {
            return Result.COOLDOWN;
        }
        if (!input.hasRecallPoint()) {
            return Result.STORE_POINT;
        }
        if (!input.sameDimension()) {
            return Result.WRONG_DIMENSION;
        }
        if (input.balance() < RECALL_PRICE) {
            return Result.INSUFFICIENT_BALANCE;
        }
        return Result.RECALL;
    }

    public record Input(
            boolean roundRunning,
            boolean recallerRole,
            boolean playerAlive,
            boolean cooldownReady,
            boolean hasRecallPoint,
            boolean sameDimension,
            int balance
    ) {
    }

    public enum Result {
        STORE_POINT,
        RECALL,
        NOT_RUNNING,
        NOT_RECALLER,
        NOT_ALIVE,
        COOLDOWN,
        WRONG_DIMENSION,
        INSUFFICIENT_BALANCE
    }
}
