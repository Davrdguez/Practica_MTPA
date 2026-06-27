package com.mtpa.server;

public class UserNotConnectedException extends RuntimeException {

    public UserNotConnectedException(String username) {
        super(username + " no esta conectado");
    }
}
