package com.mtpa.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simula un reinicio real del servidor: dos instancias de ChatServer distintas
 * apuntando al mismo directorio de datos, una detras de otra.
 */
class PersistenceIntegrationTest {

    @TempDir
    Path dataDir;

    @Test
    void unUsuarioYUnMensajeSobrevivenAUnReinicioDelServidor() throws IOException, InterruptedException {
        long accessKey;

        ChatServer firstRun = new ChatServer(0, dataDir);
        firstRun.bind();
        Thread firstThread = startAcceptLoop(firstRun);
        try (Socket socket = new Socket("localhost", firstRun.getLocalPort());
             BufferedReader in = reader(socket);
             PrintWriter out = writer(socket)) {

            out.println("REGISTER|persistente1");
            String registerResponse = in.readLine();
            accessKey = Long.parseLong(registerResponse.substring(registerResponse.lastIndexOf('|') + 1));

            out.println("LOGIN|persistente1|" + accessKey);
            in.readLine(); // OK|LOGIN
            in.readLine(); // ROOM_LIST

            out.println("JOIN_ROOM|IA");
            in.readLine(); // USER_JOINED
            in.readLine(); // END_HISTORY

            out.println("ROOM_MSG|IA|mensaje que debe sobrevivir");
            in.readLine(); // eco del propio mensaje
        } finally {
            firstRun.stop();
            firstThread.join(1000);
        }

        ChatServer secondRun = new ChatServer(0, dataDir);
        secondRun.bind();
        Thread secondThread = startAcceptLoop(secondRun);
        try (Socket socket = new Socket("localhost", secondRun.getLocalPort());
             BufferedReader in = reader(socket);
             PrintWriter out = writer(socket)) {

            out.println("LOGIN|persistente1|" + accessKey); // sin volver a registrarse
            assertEquals("OK|LOGIN|persistente1", in.readLine());
            in.readLine(); // ROOM_LIST

            out.println("JOIN_ROOM|IA");
            in.readLine(); // USER_JOINED
            String historyLine = in.readLine();

            assertTrue(historyLine.endsWith("|mensaje que debe sobrevivir"));
        } finally {
            secondRun.stop();
            secondThread.join(1000);
        }
    }

    private Thread startAcceptLoop(ChatServer server) {
        Thread thread = new Thread(server::acceptLoop, "test-server-loop");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private PrintWriter writer(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }
}
