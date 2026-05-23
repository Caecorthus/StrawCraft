package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.game.GameConstants;
import org.caecorthus.strawcraft.api.StrawDeathEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrawCorpseMetadataTest {
    @BeforeEach
    void clearMetadata() {
        StrawCorpseMetadata.clearAll();
        StrawCorpseMetadata.registerEvents();
    }

    @Test
    void officialDeathEventRecordsWatheOwnedCorpseMetadataByVictim() {
        UUID victim = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        long gameTime = 420L;

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(
                new StrawDeathEvents.OfficialDeathContext(
                        victim,
                        Optional.of(killer),
                        true,
                        GameConstants.DeathReasons.GUN,
                        gameTime,
                        true,
                        true
                )
        );

        StrawCorpseMetadata.CorpseMetadata metadata = StrawCorpseMetadata.byDeadPlayer(victim).orElseThrow();

        assertEquals(victim, metadata.deadPlayerUuid());
        assertEquals(Optional.of(killer), metadata.killerUuid());
        assertTrue(metadata.indirectAttribution());
        assertEquals(GameConstants.DeathReasons.GUN, metadata.deathReason());
        assertEquals(gameTime, metadata.gameTime());
        assertTrue(metadata.spawnBodyRequested());
        assertTrue(metadata.watheBaselineOwnsBodyAndSpectator());
    }

    @Test
    void clearAllDropsRoundScopedCorpseMetadata() {
        UUID victim = UUID.randomUUID();

        StrawDeathEvents.OFFICIAL_DEATH_COMPLETED.invoker().onOfficialDeathCompleted(
                new StrawDeathEvents.OfficialDeathContext(
                        victim,
                        Optional.empty(),
                        false,
                        StrawDeathReasons.VANILLA_DEATH,
                        11L,
                        true,
                        true
                )
        );

        StrawCorpseMetadata.clearAll();

        assertFalse(StrawCorpseMetadata.byDeadPlayer(victim).isPresent());
    }
}
