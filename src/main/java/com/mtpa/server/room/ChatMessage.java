package com.mtpa.server.room;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mensaje de un salon: quien lo escribio, cuando, y su contenido.
 */
public class ChatMessage implements Serializable {

    private final String room;
    private final String username;
    private final LocalDateTime timestamp;
    private final String content;

    public ChatMessage(String room, String username, LocalDateTime timestamp, String content) {
        this.room = room;
        this.username = username;
        this.timestamp = timestamp;
        this.content = content;
    }

    public String getRoom() {
        return room;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }
}
