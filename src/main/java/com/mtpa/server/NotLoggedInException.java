package com.mtpa.server;

public class NotLoggedInException extends RuntimeException {

    public NotLoggedInException() {
        super("Debes iniciar sesion antes de usar este comando");
    }
}
