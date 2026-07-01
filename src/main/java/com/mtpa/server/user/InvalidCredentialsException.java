package com.mtpa.server.user;

/**
 * Se lanza cuando el usuario o la clave de acceso indicados en el LOGIN no coinciden
 * con los de ningun usuario registrado.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuario o clave incorrectos");
    }
}
