package org.caecorthus.strawcraft;

public final class StrawSpectatorState {
    private StrawSpectatorState() {
    }

    public static Kind classify(Snapshot snapshot) {
        if (isWatheBaselineSpectator(snapshot.vanillaSpectator(), snapshot.creative())) {
            return Kind.WATHE_BASELINE;
        }
        if (snapshot.strawTemporarySpectator()) {
            return Kind.STRAW_TEMPORARY;
        }
        return Kind.ACTIVE;
    }

    public static boolean isWatheBaselineSpectator(boolean vanillaSpectator, boolean creative) {
        return vanillaSpectator || creative;
    }

    public enum Kind {
        ACTIVE,
        WATHE_BASELINE,
        STRAW_TEMPORARY
    }

    public record Snapshot(boolean vanillaSpectator, boolean creative, boolean strawTemporarySpectator) {
    }
}
