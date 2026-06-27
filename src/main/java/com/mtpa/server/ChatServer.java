package com.mtpa.server;

import com.mtpa.server.persistence.PersistenceManager;
import com.mtpa.server.room.RoomManager;
import com.mtpa.server.user.UserRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Acepta conexiones entrantes y lanza un hilo por cada cliente conectado.
 */
public class ChatServer {

    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
    private static final Path DEFAULT_DATA_DIR = Path.of("data");
    private static final long HEARTBEAT_TIMEOUT_MILLIS = 90_000;
    private static final long WATCHDOG_INTERVAL_SECONDS = 30;

    private final int port;
    private final UserRegistry userRegistry = new UserRegistry();
    private final RoomManager roomManager = new RoomManager();
    private final PersistenceManager persistence;
    private final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "heartbeat-watchdog");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean acceptingClients = true;
    private ServerSocket serverSocket;

    public ChatServer(int port) {
        this(port, DEFAULT_DATA_DIR);
    }

    public ChatServer(int port, Path dataDir) {
        this.port = port;
        this.persistence = new PersistenceManager(dataDir);
        persistence.loadInto(userRegistry, roomManager);
    }

    public UserRegistry getUserRegistry() {
        return userRegistry;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public PersistenceManager getPersistence() {
        return persistence;
    }

    public void registerOnline(String username, ClientHandler handler) {
        onlineUsers.put(username, handler);
    }

    public void unregisterOnline(String username) {
        if (username != null) {
            onlineUsers.remove(username);
        }
    }

    public Optional<ClientHandler> findOnline(String username) {
        return Optional.ofNullable(onlineUsers.get(username));
    }

    public int onlineUserCount() {
        return onlineUsers.size();
    }

    public void bind() throws IOException {
        serverSocket = new ServerSocket(port);
        LOGGER.info("Servidor escuchando en el puerto " + serverSocket.getLocalPort());
        watchdog.scheduleAtFixedRate(() -> disconnectStaleClients(HEARTBEAT_TIMEOUT_MILLIS),
                WATCHDOG_INTERVAL_SECONDS, WATCHDOG_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Desconecta las sesiones que no han dado señales de vida (heartbeat u otro comando) en {@code timeoutMillis}. */
    public void disconnectStaleClients(long timeoutMillis) {
        long now = System.currentTimeMillis();
        for (ClientHandler handler : onlineUsers.values()) {
            if (now - handler.getLastActivityMillis() > timeoutMillis) {
                LOGGER.info("Desconectando por inactividad (sin heartbeat) a " + handler);
                handler.disconnect();
            }
        }
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
            Thread thread = new Thread(handler, "client-" + socket.getRemoteSocketAddress());
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void start() throws IOException {
        bind();
        acceptLoop();
    }

    public void stop() throws IOException {
        watchdog.shutdownNow();
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
