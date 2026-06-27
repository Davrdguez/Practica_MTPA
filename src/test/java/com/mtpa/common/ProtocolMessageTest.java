package com.mtpa.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolMessageTest {

    @Test
    void parseaComandoSinArgumentos() {
        ProtocolMessage message = ProtocolMessage.parse("LIST_ROOMS");

        assertEquals(Command.LIST_ROOMS, message.getCommand());
        assertEquals(0, message.argCount());
    }

    @Test
    void parseaComandoConArgumentosSimples() {
        ProtocolMessage message = ProtocolMessage.parse("LOGIN|ana|1042");

        assertEquals(Command.LOGIN, message.getCommand());
        assertEquals("ana", message.arg(0));
        assertEquals("1042", message.arg(1));
    }

    @Test
    void elContenidoLibrePreservaElDelimitadorPorqueEsElUltimoCampo() {
        String contenido = "hola | que tal | todo bien?";
        ProtocolMessage message = ProtocolMessage.parse("ROOM_MSG|ia|" + contenido);

        assertEquals(Command.ROOM_MSG, message.getCommand());
        assertEquals("ia", message.arg(0));
        assertEquals(contenido, message.arg(1));
    }

    @Test
    void serializaYVuelveAParsearAlMismoMensaje() {
        ProtocolMessage original = ProtocolMessage.of(Command.PRIVATE_MSG, "bob", "hola bob | como estas");

        ProtocolMessage reparsed = ProtocolMessage.parse(original.serialize());

        assertEquals(original.getCommand(), reparsed.getCommand());
        assertEquals(original.arg(0), reparsed.arg(0));
        assertEquals(original.arg(1), reparsed.arg(1));
    }

    @Test
    void lanzaExcepcionConComandoDesconocido() {
        assertThrows(ProtocolException.class, () -> ProtocolMessage.parse("VOLAR|ana"));
    }

    @Test
    void lanzaExcepcionConLineaVacia() {
        assertThrows(ProtocolException.class, () -> ProtocolMessage.parse(""));
    }
}
