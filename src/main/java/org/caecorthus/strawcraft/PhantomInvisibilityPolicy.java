package org.caecorthus.strawcraft;

import net.minecraft.util.Identifier;

import java.util.Objects;

public final class PhantomInvisibilityPolicy {
    public static final Identifier PHANTOM_ROLE = StrawCraft.id("phantom");
    public static final String ABILITY_ID = "phantom_invisibility";
    public static final int INVISIBILITY_DURATION_TICKS = 30 * 20;
    public static final int COOLDOWN_TICKS = 90 * 20;

    private PhantomInvisibilityPolicy() {
    }

    public static Result validate(Input input) {
        Objects.requireNonNull(input, "input");
        if (!input.roundRunning()) {
            return Result.NOT_IN_ACTIVE_ROUND;
        }
        if (!input.phantomRole()) {
            return Result.NOT_PHANTOM;
        }
        if (!input.phantomAlive()) {
            return Result.PHANTOM_NOT_ALIVE;
        }
        if (!input.cooldownReady()) {
            return Result.COOLDOWN;
        }
        return Result.ALLOWED;
    }

    public enum Result {
        ALLOWED,
        NOT_IN_ACTIVE_ROUND,
        NOT_PHANTOM,
        PHANTOM_NOT_ALIVE,
        COOLDOWN;

        public boolean blocked() {
            return this != ALLOWED;
        }
    }

    public record Input(
            boolean roundRunning,
            boolean phantomRole,
            boolean phantomAlive,
            boolean cooldownReady
    ) {
        public Input withRoundRunning(boolean value) {
            return new Input(value, phantomRole, phantomAlive, cooldownReady);
        }

        public Input withPhantomRole(boolean value) {
            return new Input(roundRunning, value, phantomAlive, cooldownReady);
        }

        public Input withPhantomAlive(boolean value) {
            return new Input(roundRunning, phantomRole, value, cooldownReady);
        }

        public Input withCooldownReady(boolean value) {
            return new Input(roundRunning, phantomRole, phantomAlive, value);
        }
    }
}
