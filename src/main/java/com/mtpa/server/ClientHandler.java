package com.mtpa.server;

import com.mtpa.common.Command;
import com.mtpa.common.ProtocolException;
import com.mtpa.common.ProtocolMessage;
import com.mtpa.server.user.InvalidCredentialsException;
import com.mtpa.server.user.InvalidUsernameException;
import com.mtpa.server.user.User;
import com.mtpa.server.user.UsernameAlreadyExistsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Atiende a un cliente conectado: lee lineas del protocolo y responde.
 * El despacho hacia RoomManager se conecta aqui en cuanto exista.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ChatServer server;
    private String loggedInUsername;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        LOGGER.info("Cliente conectado: " + socket.getRemoteSocketAddress());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line, out);
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Conexion interrumpida con " + socket.getRemoteSocketAddress(), e);
        } finally {
            closeQuietly();
            LOGGER.info("Cliente desconectado: " + socket.getRemoteSocketAddress());
        }
    }

    private void handleLine(String line, PrintWriter out) {
        ProtocolMessage message;
        try {
            message = ProtocolMessage.parse(line);
        } catch (ProtocolException e) {
            out.println(ProtocolMessage.of(Command.ERROR, "UNKNOWN_COMMAND", e.getMessage()).serialize());
            return;
        }

        try {
            dispatch(message, out);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Mensaje mal formado de " + socket.getRemoteSocketAddress(), e);
            out.println(ProtocolMessage.of(Command.ERROR, "MALFORMED_MESSAGE", message.getCommand().name()).serialize());
        }
    }

    private void dispatch(ProtocolMessage message, PrintWriter out) {
        switch (message.getCommand()) {
            case HEARTBEAT -> out.println(ProtocolMessage.of(Command.HEARTBEAT_ACK).serialize());
            case REGISTER -> handleRegister(message, out);
            case LOGIN -> handleLogin(message, out);
            default -> out.println(ProtocolMessage.of(Command.OK, message.getCommand().name()).serialize());
        }
    }

    private void handleRegister(ProtocolMessage message, PrintWriter out) {
        String username = message.arg(0);
        try {
            User user = server.getUserRegistry().register(username);
            out.println(ProtocolMessage.of(Command.OK, "REGISTER", user.getUsername(), String.valueOf(user.getAccessKey())).serialize());
        } catch (InvalidUsernameException e) {
            out.println(ProtocolMessage.of(Command.ERROR, "INVALID_USERNAME", e.getMessage()).serialize());
        } catch (UsernameAlreadyExistsException e) {
            out.println(ProtocolMessage.of(Command.ERROR, "USERNAME_TAKEN", e.getMessage()).serialize());
        }
    }

    private void handleLogin(ProtocolMessage message, PrintWriter out) {
        String username = message.arg(0);
        try {
            long accessKey = Long.parseLong(message.arg(1));
            server.getUserRegistry().login(username, accessKey);
            loggedInUsername = username;
            out.println(ProtocolMessage.of(Command.OK, "LOGIN", username).serialize());
        } catch (NumberFormatException | InvalidCredentialsException e) {
            out.println(ProtocolMessage.of(Command.ERROR, "INVALID_CREDENTIALS", "Usuario o clave incorrectos").serialize());
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cerrando socket", e);
        }
    }
}
