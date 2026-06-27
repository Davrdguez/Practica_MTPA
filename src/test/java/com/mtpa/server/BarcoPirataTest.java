package com.mtpa.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BarcoPirataTest {

    @Test
    void abordaNoLanzaExcepciones() {
        BarcoPirata barco = new BarcoPirata();
        assertDoesNotThrow(barco::aborda);
    }
}
