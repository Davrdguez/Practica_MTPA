package com.mtpa.server.user;

/**
 * Se lanza al intentar registrar un nombre de usuario que ya esta en uso; el
 * enunciado exige que el nombre de usuario sea unico.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("El nombre de usuario ya existe: " + username);
    }
}
