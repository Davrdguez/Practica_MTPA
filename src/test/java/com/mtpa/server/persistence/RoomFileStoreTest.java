package com.mtpa.server.persistence;

import com.mtpa.server.room.ChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomFileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void devuelveListaVaciaSiElFicheroNoExisteAun() throws IOException {
        RoomFileStore store = new RoomFileStore(tempDir.resolve("ia.txt"));

        assertTrue(store.loadAll("IA").isEmpty());
    }

    @Test
    void guardaYRecuperaMensajesPreservandoElDelimitadorEnElContenido() throws IOException {
        RoomFileStore store = new RoomFileStore(tempDir.resolve("ia.txt"));
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 27, 10, 30, 0);
        ChatMessage original = new ChatMessage("IA", "ana123", timestamp, "hola | que tal | bien?");

        store.append(original);
        List<ChatMessage> loaded = store.loadAll("IA");

        assertEquals(1, loaded.size());
        assertEquals("ana123", loaded.get(0).getUsername());
        assertEquals(timestamp, loaded.get(0).getTimestamp());
        assertEquals("hola | que tal | bien?", loaded.get(0).getContent());
    }
}
