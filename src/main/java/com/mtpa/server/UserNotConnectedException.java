package com.mtpa.server;

/**
 * Se lanza al intentar enviar un mensaje privado a un usuario que no esta conectado
 * en ese momento; el enunciado exige que el destinatario este online.
 */
public class UserNotConnectedException extends RuntimeException {

    public UserNotConnectedException(String username) {
        super(username + " no esta conectado");
    }
}
