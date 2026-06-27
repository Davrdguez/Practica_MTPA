package com.mtpa.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminConsoleTest {

    @TempDir
    Path dataDir;

    private ChatServer server;
    private AdminConsole console;
    private final ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() throws IOException {
        server = new ChatServer(0, dataDir);
        server.bind();
        console = new AdminConsole(server, null);

        originalOut = System.out;
        System.setOut(new PrintStream(consoleOutput, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws IOException {
        System.setOut(originalOut);
        server.stop();
    }

    @Test
    void pararAltasImpideQueElServidorAcepteNuevosClientes() {
        assertTrue(server.isAcceptingClients());

        console.handleCommand("parar-altas");

        assertFalse(server.isAcceptingClients());
    }

    @Test
    void reanudarAltasVuelveAAceptarClientes() {
        console.handleCommand("parar-altas");
        console.handleCommand("reanudar-altas");

        assertTrue(server.isAcceptingClients());
    }

    @Test
    void pausarDetieneLaMensajeria() {
        assertFalse(server.isMessagingPaused());

        console.handleCommand("pausar");

        assertTrue(server.isMessagingPaused());
    }

    @Test
    void reanudarReactivaLaMensajeria() {
        console.handleCommand("pausar");
        console.handleCommand("reanudar");

        assertFalse(server.isMessagingPaused());
    }

    @Test
    void statsMuestraUsuariosConectadosYSalones() {
        console.handleCommand("stats");

        String output = consoleOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Usuarios conectados: 0"));
        assertTrue(output.contains("Salon IA"));
    }

    @Test
    void comandoDesconocidoNoRompeLaConsola() {
        boolean continueLoop = console.handleCommand("esto-no-existe");

        assertTrue(continueLoop);
    }

    @Test
    void salirCierraElServidorYDetieneElBucle() {
        boolean continueLoop = console.handleCommand("salir");

        assertFalse(continueLoop);
    }
}
