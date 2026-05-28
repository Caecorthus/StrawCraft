package org.caecorthus.strawcraft;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesNeutralWinNotificationTest {
    @Test
    void claimNotificationUsesHonestStrawCraftLangKeys() {
        UUID claimant = UUID.randomUUID();
        NoellesNeutralWinNotification.ClaimNotice notice = new NoellesNeutralWinNotification.ClaimNotice(
                claimant,
                StrawCraft.id("jester"),
                NoellesNeutralWinPolicy.JESTER_KILLED_TRIGGER,
                300L
        );

        NoellesNeutralWinNotification.Messages messages =
                NoellesNeutralWinNotification.messagesFor(notice, Text.literal("Banach"));

        assertTranslation(messages.broadcast(), "message.strawcraft.neutral_claim.broadcast");
        assertLiteralArgument(messages.broadcast(), 0, "Banach");
        assertTranslationArgument(messages.broadcast(), 1, "announcement.role.jester");
        assertTranslation(messages.actionbar(), "message.strawcraft.neutral_claim.actionbar");
        assertTranslationArgument(messages.actionbar(), 0, "announcement.role.jester");
    }

    @Test
    void collectClaimsNoOpsWhenPlayersHaveNoNeutralClaims() {
        assertTrue(NoellesNeutralWinNotification.collectClaims(Map.of()).isEmpty());

        NoellesRoleState state = new NoellesRoleState();
        assertTrue(NoellesNeutralWinNotification.collectClaims(Map.of(UUID.randomUUID(), state.neutralWinClaims())).isEmpty());
    }

    @Test
    void collectClaimsDisplaysMultipleClaimsInStableRoundOrder() {
        UUID vulture = UUID.randomUUID();
        UUID jester = UUID.randomUUID();
        NoellesRoleState vultureState = new NoellesRoleState();
        NoellesRoleState jesterState = new NoellesRoleState();
        vultureState.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                StrawCraft.id("vulture"),
                VultureBodyFeastPolicy.BODY_FEAST_TRIGGER,
                Optional.empty(),
                450L
        ));
        jesterState.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                StrawCraft.id("jester"),
                NoellesNeutralWinPolicy.JESTER_KILLED_TRIGGER,
                Optional.of(UUID.randomUUID()),
                120L
        ));

        List<NoellesNeutralWinNotification.ClaimNotice> notices = NoellesNeutralWinNotification.collectClaims(Map.of(
                vulture, vultureState.neutralWinClaims(),
                jester, jesterState.neutralWinClaims()
        ));

        assertEquals(2, notices.size());
        assertEquals(jester, notices.get(0).playerUuid());
        assertEquals(StrawCraft.id("jester"), notices.get(0).roleId());
        assertEquals(vulture, notices.get(1).playerUuid());
        assertEquals(StrawCraft.id("vulture"), notices.get(1).roleId());
    }

    @Test
    void resetStatePreventsDuplicateClaimNotificationCollection() {
        UUID claimant = UUID.randomUUID();
        NoellesRoleState state = new NoellesRoleState();
        state.recordNeutralWinClaim(new NoellesRoleState.NeutralWinClaim(
                StrawCraft.id("jester"),
                NoellesNeutralWinPolicy.JESTER_KILLED_TRIGGER,
                Optional.of(UUID.randomUUID()),
                300L
        ));
        assertEquals(1, NoellesNeutralWinNotification.collectClaims(Map.of(claimant, state.neutralWinClaims())).size());

        state.reset();

        assertTrue(NoellesNeutralWinNotification.collectClaims(Map.of(claimant, state.neutralWinClaims())).isEmpty());
    }

    @Test
    void officialFinalizeHookAnnouncesNeutralClaimsBeforeResettingRoundState() throws IOException {
        String bridge = Files.readString(
                Path.of("src/main/java/org/caecorthus/strawcraft/WatheOfficialBridge.java"),
                StandardCharsets.UTF_8
        );

        int finalizeHook = bridge.indexOf("GameEvents.ON_FINISH_FINALIZE.register");
        int announcement = bridge.indexOf("announceNoellesNeutralWinClaims(serverWorld, gameComponent)");
        int finalizeReset = bridge.lastIndexOf("resetNoellesRoleState(serverWorld);");

        assertTrue(finalizeHook >= 0);
        assertTrue(announcement > finalizeHook);
        assertTrue(finalizeReset > announcement);
    }

    @Test
    void languageFilesCoverNeutralClaimNotificationKeys() throws IOException {
        String english = Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/en_us.json"));
        String chinese = Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json"));

        for (String key : List.of(
                "message.strawcraft.neutral_claim.broadcast",
                "message.strawcraft.neutral_claim.actionbar"
        )) {
            assertTrue(english.contains("\"" + key + "\""));
            assertTrue(chinese.contains("\"" + key + "\""));
        }
    }

    private static void assertTranslation(Text text, String key) {
        TranslatableTextContent content = assertInstanceOf(TranslatableTextContent.class, text.getContent());
        assertEquals(key, content.getKey());
    }

    private static void assertTranslationArgument(Text text, int index, String key) {
        TranslatableTextContent content = assertInstanceOf(TranslatableTextContent.class, text.getContent());
        Text argument = assertInstanceOf(Text.class, content.getArgs()[index]);
        assertTranslation(argument, key);
    }

    private static void assertLiteralArgument(Text text, int index, String value) {
        TranslatableTextContent content = assertInstanceOf(TranslatableTextContent.class, text.getContent());
        Text argument = assertInstanceOf(Text.class, content.getArgs()[index]);
        assertEquals(value, argument.getString());
    }
}
