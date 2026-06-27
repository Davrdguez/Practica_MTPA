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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServerIntegrationTest {

    @TempDir
    Path dataDir;

    private ChatServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new ChatServer(0, dataDir); // puerto 0 = el SO asigna uno libre; dataDir aislado por test
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
    void desconectaUnClienteSinActividadReciente() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = reader(socket);
             PrintWriter out = writer(socket)) {

            registerAndLogin(out, in, "inactivo1");

            server.disconnectStaleClients(0); // simula que ha pasado mucho tiempo sin heartbeat

            assertNull(in.readLine()); // el servidor ha cerrado la conexion
        }
    }

    @Test
    void noDesconectaUnClienteConActividadReciente() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = reader(socket);
             PrintWriter out = writer(socket)) {

            registerAndLogin(out, in, "activo1");

            server.disconnectStaleClients(60_000); // margen amplio, la sesion es recien creada

            out.println("HEARTBEAT");
            assertEquals("HEARTBEAT_ACK", in.readLine());
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

    @Test
    void registraYLogueaUnUsuarioDeExtremoAExtremo() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("REGISTER|ana123");
            String registerResponse = in.readLine();
            assertTrue(registerResponse.startsWith("OK|REGISTER|ana123|"));
            String accessKey = registerResponse.substring(registerResponse.lastIndexOf('|') + 1);

            out.println("LOGIN|ana123|" + accessKey);
            String loginResponse = in.readLine();
            assertEquals("OK|LOGIN|ana123", loginResponse);
        }
    }

    @Test
    void rechazaLoginConClaveIncorrectaPorSocket() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("REGISTER|bob456");
            in.readLine();

            out.println("LOGIN|bob456|999999");
            String response = in.readLine();

            assertEquals("ERROR|INVALID_CREDENTIALS|Usuario o clave incorrectos", response);
        }
    }

    @Test
    void dosClientesEnElMismoSalonRecibenLosMensajesDelOtro() throws IOException {
        try (Socket socketAna = new Socket("localhost", server.getLocalPort());
             BufferedReader inAna = reader(socketAna);
             PrintWriter outAna = writer(socketAna);
             Socket socketBob = new Socket("localhost", server.getLocalPort());
             BufferedReader inBob = reader(socketBob);
             PrintWriter outBob = writer(socketBob)) {

            registerAndLogin(outAna, inAna, "ana123");
            registerAndLogin(outBob, inBob, "bob456");

            outAna.println("JOIN_ROOM|IA");
            assertEquals("USER_JOINED|IA|ana123", inAna.readLine());
            assertEquals("END_HISTORY|IA", inAna.readLine());

            outBob.println("JOIN_ROOM|IA");
            assertEquals("USER_JOINED|IA|bob456", inAna.readLine()); // ana se entera de que bob ha entrado
            assertEquals("USER_JOINED|IA|bob456", inBob.readLine());
            assertEquals("END_HISTORY|IA", inBob.readLine());

            outAna.println("ROOM_MSG|IA|hola a todos");

            String recibidoPorAna = inAna.readLine();
            String recibidoPorBob = inBob.readLine();

            assertTrue(recibidoPorAna.startsWith("ROOM_MSG_EVENT|IA|ana123|"));
            assertTrue(recibidoPorAna.endsWith("|hola a todos"));
            assertEquals(recibidoPorAna, recibidoPorBob);
        }
    }

    @Test
    void rechazaUnMensajeDeSalonDemasiadoLargo() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = reader(socket);
             PrintWriter out = writer(socket)) {

            registerAndLogin(out, in, "carla789");

            String tooLong = "a".repeat(191);
            out.println("ROOM_MSG|IA|" + tooLong);
            String response = in.readLine();

            assertTrue(response.startsWith("ERROR|MESSAGE_TOO_LONG|"));
        }
    }

    @Test
    void enviaUnMensajePrivadoAUnUsuarioConectado() throws IOException {
        try (Socket socketAna = new Socket("localhost", server.getLocalPort());
             BufferedReader inAna = reader(socketAna);
             PrintWriter outAna = writer(socketAna);
             Socket socketBob = new Socket("localhost", server.getLocalPort());
             BufferedReader inBob = reader(socketBob);
             PrintWriter outBob = writer(socketBob)) {

            registerAndLogin(outAna, inAna, "ana123");
            registerAndLogin(outBob, inBob, "bob456");

            outAna.println("PRIVATE_MSG|bob456|hola en privado");
            String recibidoPorBob = inBob.readLine();

            assertTrue(recibidoPorBob.startsWith("PRIVATE_MSG_EVENT|ana123|"));
            assertTrue(recibidoPorBob.endsWith("|hola en privado"));
        }
    }

    @Test
    void rechazaUnMensajePrivadoAUnUsuarioNoConectado() throws IOException {
        try (Socket socket = new Socket("localhost", server.getLocalPort());
             BufferedReader in = reader(socket);
             PrintWriter out = writer(socket)) {

            registerAndLogin(out, in, "ana123");

            out.println("PRIVATE_MSG|fantasma|hola?");
            String response = in.readLine();

            assertEquals("ERROR|USER_NOT_CONNECTED|fantasma no esta conectado", response);
        }
    }

    @Test
    void avisaAlOtroUsuarioCuandoSeCierraLaVentanaPrivada() throws IOException {
        try (Socket socketAna = new Socket("localhost", server.getLocalPort());
             BufferedReader inAna = reader(socketAna);
             PrintWriter outAna = writer(socketAna);
             Socket socketBob = new Socket("localhost", server.getLocalPort());
             BufferedReader inBob = reader(socketBob);
             PrintWriter outBob = writer(socketBob)) {

            registerAndLogin(outAna, inAna, "ana123");
            registerAndLogin(outBob, inBob, "bob456");

            outAna.println("PRIVATE_CLOSE|bob456");
            String response = inBob.readLine();

            assertEquals("PRIVATE_CLOSED|ana123", response);
        }
    }

    private BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private PrintWriter writer(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    private void registerAndLogin(PrintWriter out, BufferedReader in, String username) throws IOException {
        out.println("REGISTER|" + username);
        String registerResponse = in.readLine();
        String accessKey = registerResponse.substring(registerResponse.lastIndexOf('|') + 1);

        out.println("LOGIN|" + username + "|" + accessKey);
        in.readLine(); // OK|LOGIN|username
        in.readLine(); // ROOM_LIST|... (enviada automaticamente tras el login)
    }
}
