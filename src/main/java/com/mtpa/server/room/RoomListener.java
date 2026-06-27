package com.mtpa.server.room;

/**
 * Recibe los eventos de un salon al que esta suscrito (lo implementa el ClientHandler
 * de cada cliente conectado a ese salon).
 */
public interface RoomListener {

    void onMessage(ChatMessage message);

    void onUserJoined(String roomName, String username);

    void onUserLeft(String roomName, String username);
}
