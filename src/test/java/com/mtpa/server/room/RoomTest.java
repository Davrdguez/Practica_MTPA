package com.mtpa.server.room;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomTest {

    private final Room room = new Room("ia");

    @Test
    void publicaUnMensajeDentroDelLimite() {
        room.post("ana", "hola a todos");

        assertEquals(1, room.messageCount());
    }

    @Test
    void permiteUnMensajeJustoEnElLimite() {
        String exact = "a".repeat(Room.MAX_MESSAGE_LENGTH);

        room.post("ana", exact);

        assertEquals(1, room.messageCount());
    }

    @Test
    void rechazaUnMensajeQueSuperaElLimiteDeCaracteres() {
        String tooLong = "a".repeat(Room.MAX_MESSAGE_LENGTH + 1);

        assertThrows(MessageTooLongException.class, () -> room.post("ana", tooLong));
    }

    @Test
    void notificaAUnOyenteSuscritoCuandoSePublicaUnMensaje() {
        RecordingListener listener = new RecordingListener();
        room.join("ana", listener);

        room.post("ana", "hola");

        assertEquals(1, listener.messages.size());
        assertEquals("hola", listener.messages.get(0).getContent());
    }

    @Test
    void unOyentePersistenteRecibeMensajesSinContarComoUsuarioActivo() {
        RecordingListener listener = new RecordingListener();
        room.addListener(listener);

        room.post("ana", "hola");

        assertEquals(1, listener.messages.size());
        assertEquals(0, room.activeUserCount());
    }

    @Test
    void notificaLaEntradaYSalidaDeUsuariosAlRestoDeOyentes() {
        RecordingListener anaListener = new RecordingListener();
        RecordingListener bobListener = new RecordingListener();

        room.join("ana", anaListener);
        room.join("bob", bobListener);

        assertEquals(List.of("ana", "bob"), anaListener.joined);

        room.leave("bob", bobListener);

        assertEquals(List.of("bob"), anaListener.left);
    }

    @Test
    void elHistorialDelUltimoDiaExcluyeMensajesMasAntiguos() {
        ChatMessage reciente = new ChatMessage("ia", "ana", LocalDateTime.now().minusHours(2), "reciente");
        ChatMessage antiguo = new ChatMessage("ia", "ana", LocalDateTime.now().minusDays(3), "antiguo");
        room.restoreHistory(List.of(reciente, antiguo));

        List<ChatMessage> historial = room.lastDayMessages();

        assertEquals(1, historial.size());
        assertEquals("reciente", historial.get(0).getContent());
    }

    @Test
    void elHistorialDeUnDiaConcretoFiltraSoloEseDia() {
        LocalDate hace3Dias = LocalDate.now().minusDays(3);
        ChatMessage deEseDia = new ChatMessage("ia", "ana", hace3Dias.atTime(12, 0), "de ese dia");
        ChatMessage deOtroDia = new ChatMessage("ia", "ana", hace3Dias.minusDays(1).atTime(12, 0), "de otro dia");
        room.restoreHistory(List.of(deEseDia, deOtroDia));

        List<ChatMessage> historial = room.messagesBefore(hace3Dias.plusDays(1));

        assertEquals(1, historial.size());
        assertEquals("de ese dia", historial.get(0).getContent());
    }

    private static class RecordingListener implements RoomListener {
        final List<ChatMessage> messages = new ArrayList<>();
        final List<String> joined = new ArrayList<>();
        final List<String> left = new ArrayList<>();

        @Override
        public void onMessage(ChatMessage message) {
            messages.add(message);
        }

        @Override
        public void onUserJoined(String roomName, String username) {
            joined.add(username);
        }

        @Override
        public void onUserLeft(String roomName, String username) {
            left.add(username);
        }
    }
}
