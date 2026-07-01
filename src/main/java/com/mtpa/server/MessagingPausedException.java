package com.mtpa.server;

/**
 * Se lanza al intentar enviar un mensaje (de salon o privado) mientras el administrador
 * ha pausado la mensajeria desde la consola de administracion.
 */
public class MessagingPausedException extends RuntimeException {

    public MessagingPausedException() {
        super("La mensajeria esta pausada por mantenimiento");
    }
}
