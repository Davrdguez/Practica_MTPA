package com.mtpa.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el requisito de concurrencia del enunciado (al menos 10 clientes): varios clientes
 * se registran, loguean, unen al mismo salon y envian mensajes al mismo tiempo, y ninguno
 * pierde ni recibe duplicado ningun mensaje.
 */
class ChatServerConcurrencyTest {

    private static final int CLIENT_COUNT = 15;
    private static final String ROOM = "IA";

    @TempDir
    Path dataDir;

    private ChatServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new ChatServer(0, dataDir);
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
    void quinceClientesConcurrentesSeRegistranSeUnenAlMismoSalonYEnvianMensajesSinPerdidasNiDuplicados() throws Exception {
        List<ConnectedClient> clients = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(CLIENT_COUNT);
        try {
            for (int i = 0; i < CLIENT_COUNT; i++) {
                clients.add(new ConnectedClient("conc" + i, server.getLocalPort()));
            }

            // Fase 1: registro + login + union al mismo salon, todos a la vez.
            CyclicBarrier loginBarrier = new CyclicBarrier(CLIENT_COUNT);
            List<Future<Long>> loginResults = new ArrayList<>();
            for (ConnectedClient client : clients) {
                loginResults.add(pool.submit(() -> client.registerLoginAndJoin(loginBarrier, ROOM)));
            }
            Set<Long> accessKeys = new HashSet<>();
            for (Future<Long> result : loginResults) {
                accessKeys.add(result.get(15, TimeUnit.SECONDS));
            }

            assertEquals(CLIENT_COUNT, accessKeys.size(), "cada cliente debe recibir una clave de acceso unica");
            assertEquals(CLIENT_COUNT, server.onlineUserCount());
            assertEquals(CLIENT_COUNT, server.getRoomManager().getRoom(ROOM).activeUserCount());

            // Fase 2: todos envian un mensaje al salon a la vez.
            CyclicBarrier sendBarrier = new CyclicBarrier(CLIENT_COUNT);
            List<Future<?>> sendResults = new ArrayList<>();
            for (ConnectedClient client : clients) {
                sendResults.add(pool.submit(() -> {
                    client.sendRoomMessage(sendBarrier, ROOM);
                    return null;
                }));
            }
            for (Future<?> result : sendResults) {
                result.get(15, TimeUnit.SECONDS);
            }

            // Cada cliente debe recibir exactamente un mensaje distinto de cada uno de los demas, sin duplicados ni perdidas.
            // Esperar a que lleguen todos los broadcasts es tambien la forma fiable de saber que el servidor
            // ya proceso los 15 envios (el envio del cliente es asincrono respecto a su procesamiento).
            for (ConnectedClient client : clients) {
                Set<String> senders = client.collectRoomMessageSenders(ROOM, CLIENT_COUNT, 15, TimeUnit.SECONDS);
                assertEquals(CLIENT_COUNT, senders.size(),
                        client.username + " deberia recibir un mensaje de cada uno de los " + CLIENT_COUNT + " clientes");
            }

            assertEquals(CLIENT_COUNT, server.getRoomManager().getRoom(ROOM).messageCount());
        } finally {
            pool.shutdownNow();
            for (ConnectedClient client : clients) {
                client.close();
            }
        }
    }

    private static final class ConnectedClient {
        final String username;
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final LinkedBlockingQueue<String> incoming = new LinkedBlockingQueue<>();
        private final Thread readerThread;
        private volatile long accessKey;

        ConnectedClient(String username, int port) throws IOException {
            this.username = username;
            this.socket = new Socket("localhost", port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.readerThread = new Thread(this::pump, "reader-" + username);
            this.readerThread.setDaemon(true);
            this.readerThread.start();
        }

        private void pump() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    incoming.put(line);
                }
            } catch (IOException | InterruptedException ignored) {
                // el socket se cierra al terminar el test
            }
        }

        long registerLoginAndJoin(CyclicBarrier barrier, String room) throws Exception {
            barrier.await(15, TimeUnit.SECONDS);

            out.println("REGISTER|" + username);
            String registerResponse = awaitLine(l -> l.startsWith("OK|REGISTER|") || l.startsWith("ERROR|"), 15, TimeUnit.SECONDS);
            assertTrue(registerResponse.startsWith("OK|REGISTER|"), () -> username + ": registro fallido: " + registerResponse);
            accessKey = Long.parseLong(registerResponse.substring(registerResponse.lastIndexOf('|') + 1));

            out.println("LOGIN|" + username + "|" + accessKey);
            String loginResponse = awaitLine(l -> l.startsWith("OK|LOGIN|") || l.startsWith("ERROR|"), 15, TimeUnit.SECONDS);
            assertEquals("OK|LOGIN|" + username, loginResponse);
            awaitLine(l -> l.startsWith("ROOM_LIST|"), 15, TimeUnit.SECONDS); // enviada automaticamente tras el login

            out.println("JOIN_ROOM|" + room);
            awaitLine(l -> l.equals("END_HISTORY|" + room), 15, TimeUnit.SECONDS); // confirma que este cliente ya esta dentro

            return accessKey;
        }

        void sendRoomMessage(CyclicBarrier barrier, String room) throws Exception {
            barrier.await(15, TimeUnit.SECONDS);
            out.println("ROOM_MSG|" + room + "|hola desde " + username);
        }

        Set<String> collectRoomMessageSenders(String room, int expectedCount, long timeout, TimeUnit unit) throws InterruptedException {
            String prefix = "ROOM_MSG_EVENT|" + room + "|";
            Set<String> senders = new HashSet<>();
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (senders.size() < expectedCount && System.nanoTime() < deadline) {
                String line = incoming.poll(200, TimeUnit.MILLISECONDS);
                if (line != null && line.startsWith(prefix)) {
                    String rest = line.substring(prefix.length());
                    senders.add(rest.substring(0, rest.indexOf('|')));
                }
            }
            return senders;
        }

        private String awaitLine(Predicate<String> matcher, long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                String line = incoming.poll(200, TimeUnit.MILLISECONDS);
                if (line != null && matcher.test(line)) {
                    return line;
                }
            }
            throw new AssertionError("Timeout esperando la respuesta esperada para " + username);
        }

        void close() {
            readerThread.interrupt();
            try {
                socket.close();
            } catch (IOException ignored) {
                // cierre en limpieza de test
            }
        }
    }
}
