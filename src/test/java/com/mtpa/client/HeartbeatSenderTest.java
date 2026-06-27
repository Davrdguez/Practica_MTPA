package com.mtpa.client;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeartbeatSenderTest {

    @Test
    void enviaHeartbeatAlServidorAlArrancar() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            AtomicReference<String> receivedLine = new AtomicReference<>();

            Thread serverThread = new Thread(() -> {
                try (Socket accepted = serverSocket.accept();
                     BufferedReader in = new BufferedReader(
                             new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8))) {
                    receivedLine.set(in.readLine());
                } catch (IOException ignored) {
                    // socket cerrado al terminar el test
                }
            });
            serverThread.start();

            ChatClient client = new ChatClient("localhost", serverSocket.getLocalPort());
            client.connect();

            HeartbeatSender sender = new HeartbeatSender(client);
            sender.start(30);

            serverThread.join(2000);
            sender.stop();
            client.close();

            assertEquals("HEARTBEAT", receivedLine.get());
        }
    }
}
