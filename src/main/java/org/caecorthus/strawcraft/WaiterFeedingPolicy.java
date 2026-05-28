package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class WaiterFeedingPolicy {
    public static final Identifier WAITER_ROLE = StrawCraft.id("waiter");

    private WaiterFeedingPolicy() {
    }

    public static Optional<Service> chooseService(Input input) {
        if (!isWaiter(input.actorRole())
                || !input.actorAlive()
                || !input.targetAlive()
                || input.samePlayer()
                || input.serviceKind() == ServiceKind.INVALID) {
            return Optional.empty();
        }

        if (input.targetNeedsFood() && input.serviceKind().canServeFood()) {
            return Optional.of(Service.EAT);
        }
        if (input.targetNeedsDrink() && input.serviceKind().canServeDrink()) {
            return Optional.of(Service.DRINK);
        }
        return Optional.empty();
    }

    public static boolean isWaiter(Role role) {
        return StrawRoleMeaning.usesWaiterShop(role);
    }

    public record Input(
            Role actorRole,
            boolean actorAlive,
            boolean targetAlive,
            boolean samePlayer,
            ServiceKind serviceKind,
            boolean targetNeedsFood,
            boolean targetNeedsDrink
    ) {
    }

    public enum ServiceKind {
        FOOD(true, false),
        DRINK(false, true),
        SERVICE(true, true),
        INVALID(false, false);

        private final boolean servesFood;
        private final boolean servesDrink;

        ServiceKind(boolean servesFood, boolean servesDrink) {
            this.servesFood = servesFood;
            this.servesDrink = servesDrink;
        }

        public boolean canServeFood() {
            return servesFood;
        }

        public boolean canServeDrink() {
            return servesDrink;
        }
    }

    public enum Service {
        EAT,
        DRINK
    }
}
