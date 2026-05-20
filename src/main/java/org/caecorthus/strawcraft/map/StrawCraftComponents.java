package org.caecorthus.strawcraft.map;

import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;

public final class StrawCraftComponents implements ScoreboardComponentInitializer {
    @Override
    public void registerScoreboardComponentFactories(@NotNull ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(StrawMapVotingComponent.KEY, StrawMapVotingComponent::new);
    }
}
