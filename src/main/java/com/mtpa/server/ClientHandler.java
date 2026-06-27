package com.mtpa.server;

import com.mtpa.common.Command;
import com.mtpa.common.ProtocolException;
import com.mtpa.common.ProtocolMessage;

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
 * El despacho real hacia UserRegistry/RoomManager se conecta aqui en cuanto existan.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ChatServer server;

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

        if (message.getCommand() == Command.HEARTBEAT) {
            out.println(ProtocolMessage.of(Command.HEARTBEAT_ACK).serialize());
            return;
        }

        // Pendiente: delegar en UserRegistry / RoomManager segun el comando.
        out.println(ProtocolMessage.of(Command.OK, message.getCommand().name()).serialize());
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cerrando socket", e);
        }
    }
}
