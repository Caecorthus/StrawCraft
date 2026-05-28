package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoellesRoleWelcomeTest {
    @Test
    void rewrittenWelcomeAnnouncesTheActualRoleAndGoalWithLangKeys() {
        NoellesRoleWelcome.Messages messages = NoellesRoleWelcome.messagesFor(
                role(StrawCraft.id("bomber"), false, true)
        );

        assertTranslation(messages.role(), "message.strawcraft.role_rewrite.role");
        assertTranslationArgument(messages.role(), 0, "announcement.role.bomber");
        assertTranslation(messages.goal(), "message.strawcraft.role_rewrite.goal");
        assertTranslationArgument(messages.goal(), 0, "announcement.goal.bomber");
        assertTranslation(messages.actionbar(), "message.strawcraft.role_rewrite.actionbar");
        assertTranslationArgument(messages.actionbar(), 0, "announcement.role.bomber");
    }

    @Test
    void languageFilesCoverRewrittenWelcomeCorrectionKeys() throws IOException {
        String english = Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/en_us.json"));
        String chinese = Files.readString(Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json"));

        for (String key : java.util.List.of(
                "message.strawcraft.role_rewrite.role",
                "message.strawcraft.role_rewrite.goal",
                "message.strawcraft.role_rewrite.actionbar"
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

    private static Role role(net.minecraft.util.Identifier id, boolean innocent, boolean killerTools) {
        return new Role(id, 0xFFFFFF, innocent, killerTools, Role.MoodType.REAL, 200, false);
    }
}
