package com.mtpa.server;

import com.mtpa.server.room.RoomManager;
import com.mtpa.server.user.UserRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Acepta conexiones entrantes y lanza un hilo por cada cliente conectado.
 */
public class ChatServer {

    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());

    private final int port;
    private final UserRegistry userRegistry = new UserRegistry();
    private final RoomManager roomManager = new RoomManager();
    private volatile boolean acceptingClients = true;
    private ServerSocket serverSocket;

    public ChatServer(int port) {
        this.port = port;
    }

    public UserRegistry getUserRegistry() {
        return userRegistry;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void bind() throws IOException {
        serverSocket = new ServerSocket(port);
        LOGGER.info("Servidor escuchando en el puerto " + serverSocket.getLocalPort());
    }

    public void acceptLoop() {
        while (serverSocket != null && !serverSocket.isClosed()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                break; // serverSocket cerrado durante el accept()
            }

            if (!acceptingClients) {
                LOGGER.info("Conexion rechazada (servidor no admite mas clientes): " + socket.getRemoteSocketAddress());
                closeQuietly(socket);
                continue;
            }

            ClientHandler handler = new ClientHandler(socket, this);
            new Thread(handler, "client-" + socket.getRemoteSocketAddress()).start();
        }
    }

    public void start() throws IOException {
        bind();
        acceptLoop();
    }

    public void stop() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    public boolean isAcceptingClients() {
        return acceptingClients;
    }

    public void setAcceptingClients(boolean acceptingClients) {
        this.acceptingClients = acceptingClients;
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.warning("Error cerrando socket rechazado: " + e.getMessage());
        }
    }
}
