package org.caecorthus.strawcraft.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StrawWinEvents {
    public static final Event<CollectWinContributions> COLLECT_WIN_CONTRIBUTIONS = EventFactory.createArrayBacked(
            CollectWinContributions.class,
            listeners -> (context, builder) -> {
                for (CollectWinContributions listener : listeners) {
                    listener.collectWinContributions(context, builder);
                }
            }
    );

    private StrawWinEvents() {
    }

    public static WinContribution collect(WinContext context) {
        WinContribution.Builder builder = WinContribution.builder();
        COLLECT_WIN_CONTRIBUTIONS.invoker().collectWinContributions(context, builder);
        return builder.build();
    }

    public interface CollectWinContributions {
        void collectWinContributions(WinContext context, WinContribution.Builder contribution);
    }

    public enum DefaultWin {
        NONE,
        KILLERS,
        PASSENGERS,
        TIME,
        LOOSE_END
    }

    public record WinContext(Identifier worldId, DefaultWin defaultWin) {
        public WinContext {
            Objects.requireNonNull(worldId, "worldId");
            Objects.requireNonNull(defaultWin, "defaultWin");
        }
    }

    public record ExtraWinner(UUID playerUuid, Identifier roleId) {
        public ExtraWinner {
            Objects.requireNonNull(playerUuid, "playerUuid");
            Objects.requireNonNull(roleId, "roleId");
        }
    }

    public record WinContribution(
            Set<ExtraWinner> extraWinners,
            boolean suppressDefaultWin,
            Optional<DefaultWin> replacementDefaultWin
    ) {
        public WinContribution {
            extraWinners = Set.copyOf(extraWinners);
            Objects.requireNonNull(replacementDefaultWin, "replacementDefaultWin");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static WinContribution none() {
            return builder().build();
        }

        public static final class Builder {
            private final Set<ExtraWinner> extraWinners = new HashSet<>();
            private boolean suppressDefaultWin;
            private Optional<DefaultWin> replacementDefaultWin = Optional.empty();

            private Builder() {
            }

            public Builder addExtraWinner(UUID playerUuid, Identifier roleId) {
                extraWinners.add(new ExtraWinner(playerUuid, roleId));
                return this;
            }

            public Builder suppressDefaultWin() {
                suppressDefaultWin = true;
                return this;
            }

            public Builder replaceDefaultWin(DefaultWin defaultWin) {
                replacementDefaultWin = Optional.of(defaultWin);
                return this;
            }

            public WinContribution build() {
                // The contribution is intentionally richer than official Wathe 1.3.2 can consume today.
                // 这个贡献结果故意比官方 Wathe 1.3.2 现在能消费的内容更完整，方便之后接入真正胜利展示。
                return new WinContribution(extraWinners, suppressDefaultWin, replacementDefaultWin);
            }
        }
    }
}
