package org.caecorthus.strawcraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BartenderDefenseVialResourceTest {
    private static final Path MODEL = Path.of("src/main/resources/assets/strawcraft/models/item/defense_vial.json");
    private static final Path ENGLISH = Path.of("src/main/resources/assets/strawcraft/lang/en_us.json");
    private static final Path CHINESE = Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json");

    @Test
    void defenseVialItemUsesRealStrawCraftRegistryId() {
        assertEquals(StrawCraft.id("defense_vial"), StrawCraftItems.DEFENSE_VIAL_ID);
    }

    @Test
    void defenseVialItemModelIsValidGeneratedItemJson() throws IOException {
        JsonObject model = JsonParser.parseString(Files.readString(MODEL)).getAsJsonObject();

        assertEquals("item/generated", model.get("parent").getAsString());
        assertEquals("minecraft:item/potion", model.getAsJsonObject("textures").get("layer0").getAsString());
    }

    @Test
    void languageFilesNameDefenseVialItemAndRemainValidJson() throws IOException {
        JsonObject english = JsonParser.parseString(Files.readString(ENGLISH)).getAsJsonObject();
        JsonObject chinese = JsonParser.parseString(Files.readString(CHINESE)).getAsJsonObject();

        assertTrue(english.has("item.strawcraft.defense_vial"));
        assertTrue(chinese.has("item.strawcraft.defense_vial"));
    }
}
