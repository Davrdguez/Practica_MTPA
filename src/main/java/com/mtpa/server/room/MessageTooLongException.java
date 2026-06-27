package com.mtpa.server.room;

public class MessageTooLongException extends RuntimeException {

    public MessageTooLongException(int length) {
        super("El mensaje supera el limite de " + Room.MAX_MESSAGE_LENGTH + " caracteres (longitud: " + length + ")");
    }
}
