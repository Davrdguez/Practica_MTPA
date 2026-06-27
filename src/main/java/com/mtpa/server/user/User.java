package com.mtpa.server.user;

import java.io.Serializable;

/**
 * Usuario registrado en el servidor: nombre unico y clave de acceso autonumerica unica.
 */
public class User implements Serializable {

    private final String username;
    private final long accessKey;

    public User(String username, long accessKey) {
        this.username = username;
        this.accessKey = accessKey;
    }

    public String getUsername() {
        return username;
    }

    public long getAccessKey() {
        return accessKey;
    }
}
