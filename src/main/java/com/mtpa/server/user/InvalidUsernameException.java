package com.mtpa.server.user;

public class InvalidUsernameException extends RuntimeException {

    public InvalidUsernameException(String username) {
        super("Nombre de usuario invalido: " + username);
    }
}
