package com.mtpa.server.persistence;

import com.mtpa.server.room.ChatMessage;
import com.mtpa.server.room.RoomListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Oyente permanente de un salon cuyo unico cometido es dejar en disco cada
 * mensaje nuevo, sin contar como un usuario activo del salon.
 */
public class PersistingRoomListener implements RoomListener {

    private static final Logger LOGGER = Logger.getLogger(PersistingRoomListener.class.getName());

    private final RoomFileStore store;

    public PersistingRoomListener(RoomFileStore store) {
        this.store = store;
    }

    @Override
    public void onMessage(ChatMessage message) {
        try {
            store.append(message);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo persistir el mensaje en " + message.getRoom(), e);
        }
    }

    @Override
    public void onUserJoined(String roomName, String username) {
        // No se persiste la presencia de usuarios.
    }

    @Override
    public void onUserLeft(String roomName, String username) {
        // No se persiste la presencia de usuarios.
    }
}
