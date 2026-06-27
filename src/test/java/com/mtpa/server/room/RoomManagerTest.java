package com.mtpa.server.room;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomManagerTest {

    private final RoomManager manager = new RoomManager();

    @Test
    void creaLosCincoSalonesPredefinidos() {
        assertEquals(5, manager.allRooms().size());
        for (String name : RoomManager.DEFAULT_ROOMS) {
            assertEquals(name, manager.getRoom(name).getName());
        }
    }

    @Test
    void lanzaExcepcionSiLaSalaNoExiste() {
        assertThrows(RoomNotFoundException.class, () -> manager.getRoom("Inexistente"));
    }
}
