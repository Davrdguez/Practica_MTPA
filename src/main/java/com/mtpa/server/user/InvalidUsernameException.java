package com.mtpa.server.user;

/**
 * Se lanza cuando un nombre de usuario no cumple el formato exigido en el registro
 * (letras, digitos o guion bajo, entre 3 y 20 caracteres).
 */
public class InvalidUsernameException extends RuntimeException {

    public InvalidUsernameException(String username) {
        super("Nombre de usuario invalido: " + username);
    }
}
