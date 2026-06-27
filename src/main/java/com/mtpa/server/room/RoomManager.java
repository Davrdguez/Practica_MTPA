package com.mtpa.server.room;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene los salones del servidor, creados a partir de una lista predefinida.
 */
public class RoomManager {

    public static final List<String> DEFAULT_ROOMS = List.of("IA", "Deportes", "Therian", "Manga", "UEMC");

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomManager() {
        for (String roomName : DEFAULT_ROOMS) {
            rooms.put(roomName, new Room(roomName));
        }
    }

    public Room getRoom(String name) {
        Room room = rooms.get(name);
        if (room == null) {
            throw new RoomNotFoundException(name);
        }
        return room;
    }

    public Collection<Room> allRooms() {
        return rooms.values();
    }
}
