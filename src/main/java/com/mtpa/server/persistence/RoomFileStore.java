package com.mtpa.server.persistence;

import com.mtpa.server.room.ChatMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste el historial de un salon en un fichero de texto: una linea por mensaje
 * con el formato {@code timestamp|username|contenido}. El contenido es siempre el
 * ultimo campo de la linea, por lo que puede incluir el caracter '|' sin romper el parseo.
 */
public class RoomFileStore {

    private final Path filePath;

    public RoomFileStore(Path filePath) {
        this.filePath = filePath;
    }

    public synchronized List<ChatMessage> loadAll(String roomName) throws IOException {
        if (!Files.exists(filePath)) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 3);
            LocalDateTime timestamp = LocalDateTime.parse(parts[0]);
            messages.add(new ChatMessage(roomName, parts[1], timestamp, parts[2]));
        }
        return messages;
    }

    public synchronized void append(ChatMessage message) throws IOException {
        Files.createDirectories(filePath.getParent());
        String line = message.getTimestamp() + "|" + message.getUsername() + "|" + message.getContent() + System.lineSeparator();
        Files.writeString(filePath, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
