package com.mtpa.server;

/**
 * Se lanza cuando un cliente envia un comando que exige sesion iniciada (por ejemplo,
 * unirse a un salon) antes de haber hecho LOGIN.
 */
public class NotLoggedInException extends RuntimeException {

    public NotLoggedInException() {
        super("Debes iniciar sesion antes de usar este comando");
    }
}
