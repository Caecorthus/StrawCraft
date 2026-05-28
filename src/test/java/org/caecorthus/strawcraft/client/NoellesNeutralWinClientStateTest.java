package org.caecorthus.strawcraft.client;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.caecorthus.strawcraft.JesterWinPolicy;
import org.caecorthus.strawcraft.NoellesNeutralWinPolicy;
import org.caecorthus.strawcraft.NoellesNeutralWinResultPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesNeutralWinClientStateTest {
    @AfterEach
    void clearClientState() {
        NoellesNeutralWinClientState.clear();
    }

    @Test
    void neutralWinPayloadReplacesLooseEndTextWithSpecificRoleVictory() {
        NoellesNeutralWinClientState.accept(new NoellesNeutralWinResultPayload(
                UUID.randomUUID(),
                JesterWinPolicy.JESTER_ROLE
        ));

        Optional<Text> text = NoellesNeutralWinClientState.endTextFor(
                GameFunctions.WinStatus.LOOSE_END,
                Text.literal("Banach")
        );

        assertTrue(text.isPresent());
        TranslatableTextContent content = assertInstanceOf(TranslatableTextContent.class, text.orElseThrow().getContent());
        assertEquals("announcement.win.strawcraft.jester", content.getKey());
    }

    @Test
    void clearPayloadPreventsStaleNeutralRoleTextFromNextLooseEndRound() {
        NoellesNeutralWinClientState.accept(new NoellesNeutralWinResultPayload(
                UUID.randomUUID(),
                JesterWinPolicy.JESTER_ROLE
        ));

        NoellesNeutralWinClientState.accept(new NoellesNeutralWinResultPayload(
                new UUID(0L, 0L),
                NoellesNeutralWinPolicy.clearWinnerScreenRole()
        ));

        assertTrue(NoellesNeutralWinClientState.endTextFor(
                GameFunctions.WinStatus.LOOSE_END,
                Text.literal("Loose End")
        ).isEmpty());
    }
}
