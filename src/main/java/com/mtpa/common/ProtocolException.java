package com.mtpa.common;

/**
 * Indica que una linea recibida no cumple el formato del protocolo de aplicacion.
 */
public class ProtocolException extends RuntimeException {

    public ProtocolException(String message) {
        super(message);
    }
}
