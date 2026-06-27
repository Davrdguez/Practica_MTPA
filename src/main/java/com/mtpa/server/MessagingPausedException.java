package com.mtpa.server;

public class MessagingPausedException extends RuntimeException {

    public MessagingPausedException() {
        super("La mensajeria esta pausada por mantenimiento");
    }
}
