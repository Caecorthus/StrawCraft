package org.caecorthus.strawcraft;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import org.caecorthus.strawcraft.map.StrawRoomAssignment;
import org.caecorthus.strawcraft.map.StrawRoomConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class AttendantRoomManifest {
    private static final String TITLE = "Room Manifest";
    private static final String AUTHOR = "StrawCraft";
    private static final String EMPTY_ROOM_TEXT = "No assigned players";

    private AttendantRoomManifest() {
    }

    public static void giveToAssignedAttendants(
            List<ServerPlayerEntity> participants,
            Map<UUID, Role> roles,
            Map<UUID, StrawRoomAssignment> assignments,
            List<StrawRoomConfig> rooms
    ) {
        if (assignments.isEmpty() || rooms.isEmpty()) {
            return;
        }

        Map<UUID, String> displayNames = participants.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ServerPlayerEntity::getUuid,
                        AttendantRoomManifest::displayNameFor
                ));
        ItemStack manifest = createBook(entries(rooms, assignments, displayNames::get));
        for (ServerPlayerEntity player : participants) {
            UUID playerId = player.getUuid();
            if (shouldReceiveManifest(roles.get(playerId), player.isAlive(), assignments.containsKey(playerId))) {
                player.giveItemStack(manifest.copy());
            }
        }
    }

    static boolean shouldReceiveManifest(Role role, boolean alive, boolean assignedRoom) {
        return alive
                && assignedRoom
                && StrawRoleMeaning.receivesAttendantRoomManifest(role);
    }

    static List<RoomEntry> entries(
            List<StrawRoomConfig> rooms,
            Map<UUID, StrawRoomAssignment> assignments,
            Function<UUID, String> displayNameResolver
    ) {
        LinkedHashMap<StrawRoomConfig, List<String>> playersByRoom = new LinkedHashMap<>();
        for (StrawRoomConfig room : rooms) {
            playersByRoom.put(room, new ArrayList<>());
        }
        for (Map.Entry<UUID, StrawRoomAssignment> assignment : assignments.entrySet()) {
            List<String> roomPlayers = playersByRoom.get(assignment.getValue().room());
            if (roomPlayers != null) {
                String displayName = displayNameResolver.apply(assignment.getKey());
                roomPlayers.add(displayName != null ? displayName : assignment.getKey().toString());
            }
        }
        return playersByRoom.entrySet().stream()
                .map(entry -> new RoomEntry(entry.getKey().keyName(), List.copyOf(entry.getValue())))
                .toList();
    }

    static ItemStack createBook(List<RoomEntry> rooms) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(
                RawFilteredPair.of(TITLE),
                AUTHOR,
                0,
                pagesFor(rooms).stream()
                        .map(RawFilteredPair::of)
                        .toList(),
                true
        ));
        return stack;
    }

    static List<Text> pagesFor(List<RoomEntry> rooms) {
        return rooms.stream()
                .map(AttendantRoomManifest::pageFor)
                .toList();
    }

    private static Text pageFor(RoomEntry room) {
        StringBuilder page = new StringBuilder(room.roomName()).append('\n');
        if (room.playerDisplayNames().isEmpty()) {
            page.append(EMPTY_ROOM_TEXT);
        } else {
            for (String playerName : room.playerDisplayNames()) {
                page.append("- ").append(playerName).append('\n');
            }
            page.setLength(page.length() - 1);
        }
        return Text.literal(page.toString());
    }

    private static String displayNameFor(ServerPlayerEntity player) {
        Text displayName = player.getDisplayName();
        if (displayName != null) {
            return displayName.getString();
        }
        return player.getName().getString();
    }

    public record RoomEntry(String roomName, List<String> playerDisplayNames) {
    }
}
