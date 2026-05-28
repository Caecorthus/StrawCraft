package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.map.StrawRoomAssignment;
import org.caecorthus.strawcraft.map.StrawRoomConfig;
import org.caecorthus.strawcraft.map.StrawRoomSpawnPoint;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendantRoomManifestTest {
    @Test
    void manifestBookUsesRoomOrderNamesAndAssignedDisplayNames() {
        UUID alice = new UUID(0, 1);
        UUID bob = new UUID(0, 2);
        UUID cyrus = new UUID(0, 3);
        StrawRoomConfig cabin = room("cabin_a", "Cabin A");
        StrawRoomConfig dining = room("dining_car", "Dining Car");
        Map<UUID, StrawRoomAssignment> assignments = new LinkedHashMap<>();
        assignments.put(cyrus, new StrawRoomAssignment(dining, dining.spawns().getFirst()));
        assignments.put(alice, new StrawRoomAssignment(cabin, cabin.spawns().getFirst()));
        assignments.put(bob, new StrawRoomAssignment(cabin, cabin.spawns().getFirst()));

        List<Text> pages = AttendantRoomManifest.pagesFor(AttendantRoomManifest.entries(
                List.of(cabin, dining),
                assignments,
                Map.of(alice, "Alice", bob, "Bob", cyrus, "Cyrus")::get
        ));

        assertEquals(List.of(
                "Cabin A\n- Alice\n- Bob",
                "Dining Car\n- Cyrus"
        ), pages.stream().map(Text::getString).toList());
    }

    @Test
    void runtimeBookUsesVanillaWrittenBookComponents() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/org/caecorthus/strawcraft/AttendantRoomManifest.java"
        ));

        assertTrue(source.contains("new ItemStack(Items.WRITTEN_BOOK)"));
        assertTrue(source.contains("DataComponentTypes.WRITTEN_BOOK_CONTENT"));
        assertFalse(source.contains("noellesroles"));
        assertFalse(source.contains("parox"));
        assertFalse(source.contains("MapEnhancementsWorldComponent"));
    }

    @Test
    void onlyAliveAssignedAttendantsReceiveManifest() {
        Role attendant = new Role(StrawCraft.id("attendant"), 0xFFFFFF, true, false, Role.MoodType.REAL, 200, false);
        Role bodyguard = new Role(StrawCraft.id("bodyguard"), 0xFFFFFF, true, false, Role.MoodType.REAL, 200, false);

        assertTrue(AttendantRoomManifest.shouldReceiveManifest(attendant, true, true));
        assertFalse(AttendantRoomManifest.shouldReceiveManifest(attendant, false, true));
        assertFalse(AttendantRoomManifest.shouldReceiveManifest(attendant, true, false));
        assertFalse(AttendantRoomManifest.shouldReceiveManifest(bodyguard, true, true));
        assertFalse(AttendantRoomManifest.shouldReceiveManifest(null, true, true));
    }

    private static StrawRoomConfig room(String id, String keyName) {
        return new StrawRoomConfig(id, keyName, 2, List.of(new StrawRoomSpawnPoint(1.0, 2.0, 3.0, 0.0F, 0.0F)));
    }
}
