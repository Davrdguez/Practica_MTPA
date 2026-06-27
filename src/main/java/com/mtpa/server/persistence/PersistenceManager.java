package com.mtpa.server.persistence;

import com.mtpa.server.room.Room;
import com.mtpa.server.room.RoomManager;
import com.mtpa.server.user.User;
import com.mtpa.server.user.UserRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto unico de acceso a la persistencia: carga el estado guardado al arrancar
 * y deja los salones conectados a su fichero para que cada mensaje nuevo se guarde al vuelo.
 */
public class PersistenceManager {

    private static final Logger LOGGER = Logger.getLogger(PersistenceManager.class.getName());

    private final Path dataDir;
    private final UserFileStore userFileStore;
    private final Map<String, RoomFileStore> roomFileStores = new ConcurrentHashMap<>();

    public PersistenceManager(Path dataDir) {
        this.dataDir = dataDir;
        this.userFileStore = new UserFileStore(dataDir.resolve("users.txt"));
    }

    public UserFileStore getUserFileStore() {
        return userFileStore;
    }

    public RoomFileStore getRoomFileStore(String roomName) {
        return roomFileStores.computeIfAbsent(roomName,
                name -> new RoomFileStore(dataDir.resolve("rooms").resolve(name + ".txt")));
    }

    /** Carga usuarios y salones guardados, y deja cada salon escuchando para persistir mensajes nuevos. */
    public void loadInto(UserRegistry userRegistry, RoomManager roomManager) {
        try {
            for (User user : userFileStore.loadAll()) {
                userRegistry.restore(user);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo cargar la lista de usuarios persistida", e);
        }

        for (Room room : roomManager.allRooms()) {
            RoomFileStore store = getRoomFileStore(room.getName());
            try {
                room.restoreHistory(store.loadAll(room.getName()));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "No se pudo cargar el historial del salon " + room.getName(), e);
            }
            room.addListener(new PersistingRoomListener(store));
        }
    }
}
