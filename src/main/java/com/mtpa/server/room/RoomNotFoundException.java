package com.mtpa.server.room;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String roomName) {
        super("Sala no encontrada: " + roomName);
    }
}
