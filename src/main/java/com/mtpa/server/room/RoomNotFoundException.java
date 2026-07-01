package com.mtpa.server.room;

/**
 * Se lanza cuando se referencia un salon cuyo nombre no existe en el {@link RoomManager}.
 */
public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String roomName) {
        super("Sala no encontrada: " + roomName);
    }
}
