package com.mtpa.server.user;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuario o clave incorrectos");
    }
}
