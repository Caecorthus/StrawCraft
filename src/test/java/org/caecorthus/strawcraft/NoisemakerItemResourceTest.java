package org.caecorthus.strawcraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoisemakerItemResourceTest {
    private static final Path MODEL = Path.of("src/main/resources/assets/strawcraft/models/item/noisemaker.json");
    private static final Path ENGLISH = Path.of("src/main/resources/assets/strawcraft/lang/en_us.json");
    private static final Path CHINESE = Path.of("src/main/resources/assets/strawcraft/lang/zh_cn.json");

    @Test
    void noisemakerItemUsesRealStrawCraftRegistryId() {
        assertEquals(StrawCraft.id("noisemaker"), StrawCraftItems.NOISEMAKER_ID);
    }

    @Test
    void noisemakerItemModelIsValidGeneratedItemJson() throws IOException {
        JsonObject model = JsonParser.parseString(Files.readString(MODEL)).getAsJsonObject();

        assertEquals("item/generated", model.get("parent").getAsString());
        assertEquals("minecraft:item/goat_horn", model.getAsJsonObject("textures").get("layer0").getAsString());
    }

    @Test
    void languageFilesNameNoisemakerItemAndRemainValidJson() throws IOException {
        JsonObject english = JsonParser.parseString(Files.readString(ENGLISH)).getAsJsonObject();
        JsonObject chinese = JsonParser.parseString(Files.readString(CHINESE)).getAsJsonObject();

        assertTrue(english.has("item.strawcraft.noisemaker"));
        assertTrue(chinese.has("item.strawcraft.noisemaker"));
    }
}
