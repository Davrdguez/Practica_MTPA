package com.mtpa.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatServerIntegrationTest {

    private ChatServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new ChatServer(0); // puerto 0 = el sistema operativo asigna uno libre
        server.bind();

        Thread serverThread = new Thread(server::acceptLoop, "test-server-loop");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.stop();
    }

    @Test
    void respondeHeartbeatAckAUnCliente() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("HEARTBEAT");
            String response = in.readLine();

            assertEquals("HEARTBEAT_ACK", response);
        }
    }

    @Test
    void rechazaNuevasConexionesCuandoElServidorNoAceptaClientes() throws IOException {
        server.setAcceptingClients(false);

        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String response = in.readLine();

            assertNull(response); // el servidor cierra la conexion sin responder nada
        }
    }
}
