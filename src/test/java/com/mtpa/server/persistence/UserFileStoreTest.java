package com.mtpa.server.persistence;

import com.mtpa.server.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserFileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void devuelveListaVaciaSiElFicheroNoExisteAun() throws IOException {
        UserFileStore store = new UserFileStore(tempDir.resolve("users.txt"));

        assertTrue(store.loadAll().isEmpty());
    }

    @Test
    void guardaYRecuperaUsuarios() throws IOException {
        UserFileStore store = new UserFileStore(tempDir.resolve("users.txt"));

        store.append(new User("ana123", 1000L));
        store.append(new User("bob456", 1001L));

        List<User> loaded = store.loadAll();

        assertEquals(2, loaded.size());
        assertEquals("ana123", loaded.get(0).getUsername());
        assertEquals(1000L, loaded.get(0).getAccessKey());
        assertEquals("bob456", loaded.get(1).getUsername());
        assertEquals(1001L, loaded.get(1).getAccessKey());
    }
}
