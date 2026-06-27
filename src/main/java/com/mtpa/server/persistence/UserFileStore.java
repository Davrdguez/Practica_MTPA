package com.mtpa.server.persistence;

import com.mtpa.server.user.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste la lista de usuarios en un fichero de texto: una linea por usuario
 * con el formato {@code username|clave}. El nombre de usuario nunca contiene
 * '|' (se valida en el registro), asi que no hace falta tratamiento especial.
 */
public class UserFileStore {

    private final Path filePath;

    public UserFileStore(Path filePath) {
        this.filePath = filePath;
    }

    public synchronized List<User> loadAll() throws IOException {
        if (!Files.exists(filePath)) {
            return List.of();
        }

        List<User> users = new ArrayList<>();
        for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            users.add(new User(parts[0], Long.parseLong(parts[1])));
        }
        return users;
    }

    public synchronized void append(User user) throws IOException {
        Files.createDirectories(filePath.getParent());
        String line = user.getUsername() + "|" + user.getAccessKey() + System.lineSeparator();
        Files.writeString(filePath, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
