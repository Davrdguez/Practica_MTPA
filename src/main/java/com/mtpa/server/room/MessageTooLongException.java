package com.mtpa.server.room;

/**
 * Se lanza cuando un mensaje de salon supera el limite de {@link Room#MAX_MESSAGE_LENGTH}
 * caracteres exigido por el enunciado.
 */
public class MessageTooLongException extends RuntimeException {

    public MessageTooLongException(int length) {
        super("El mensaje supera el limite de " + Room.MAX_MESSAGE_LENGTH + " caracteres (longitud: " + length + ")");
    }
}
