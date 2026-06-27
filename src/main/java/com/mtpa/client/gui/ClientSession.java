package com.mtpa.client.gui;

import com.mtpa.client.ChatClient;
import com.mtpa.client.HeartbeatSender;
import com.mtpa.common.Command;
import com.mtpa.common.ProtocolException;
import com.mtpa.common.ProtocolMessage;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sesion de un cliente GUI: mantiene la conexion con el servidor y enruta cada
 * evento entrante hacia la ventana (salon o chat privado) a la que corresponde.
 * Todo el despacho hacia componentes Swing ocurre en el Event Dispatch Thread.
 */
public class ClientSession {

    private static final Logger LOGGER = Logger.getLogger(ClientSession.class.getName());
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;

    private final ChatClient client;
    private final HeartbeatSender heartbeatSender;
    private final Map<String, RoomFrame> openRooms = new ConcurrentHashMap<>();
    private final Map<String, PrivateChatFrame> openPrivateChats = new ConcurrentHashMap<>();
    private volatile String username;
    private volatile BiConsumer<Boolean, String> pendingAuthCallback;
    private RoomListFrame roomListFrame;

    public ClientSession(String host, int port) throws IOException {
        client = new ChatClient(host, port);
        client.connect();
        heartbeatSender = new HeartbeatSender(client);

        Thread readerThread = new Thread(this::readLoop, "gui-server-listener");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public String getUsername() {
        return username;
    }

    public void setRoomListFrame(RoomListFrame frame) {
        this.roomListFrame = frame;
    }

    public void startHeartbeat() {
        heartbeatSender.start(HEARTBEAT_INTERVAL_SECONDS);
    }

    public void register(String requestedUsername, BiConsumer<Boolean, String> callback) {
        pendingAuthCallback = callback;
        client.send(ProtocolMessage.of(Command.REGISTER, requestedUsername).serialize());
    }

    public void login(String usernameToLogin, String accessKey, BiConsumer<Boolean, String> callback) {
        pendingAuthCallback = callback;
        client.send(ProtocolMessage.of(Command.LOGIN, usernameToLogin, accessKey).serialize());
    }

    public void refreshRoomList() {
        client.send(ProtocolMessage.of(Command.LIST_ROOMS).serialize());
    }

    public void joinRoom(String room, RoomFrame frame) {
        openRooms.put(room, frame);
        client.send(ProtocolMessage.of(Command.JOIN_ROOM, room).serialize());
    }

    public void leaveRoom(String room) {
        openRooms.remove(room);
        client.send(ProtocolMessage.of(Command.LEAVE_ROOM, room).serialize());
    }

    public void sendRoomMessage(String room, String content) {
        client.send(ProtocolMessage.of(Command.ROOM_MSG, room, content).serialize());
    }

    public void requestHistory(String room, LocalDate beforeDate) {
        client.send(ProtocolMessage.of(Command.HISTORY_REQUEST, room, beforeDate.toString()).serialize());
    }

    public void openOrFocusPrivateChat(String targetUser) {
        PrivateChatFrame frame = openPrivateChats.computeIfAbsent(targetUser, this::createPrivateChatFrame);
        frame.setVisible(true);
        frame.toFront();
    }

    public void sendPrivateMessage(String targetUser, String content) {
        client.send(ProtocolMessage.of(Command.PRIVATE_MSG, targetUser, content).serialize());
    }

    public void closePrivateChat(String targetUser) {
        openPrivateChats.remove(targetUser);
        client.send(ProtocolMessage.of(Command.PRIVATE_CLOSE, targetUser).serialize());
    }

    public void shutdown() {
        heartbeatSender.stop();
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cerrando la conexion", e);
        }
    }

    private PrivateChatFrame createPrivateChatFrame(String otherUser) {
        return new PrivateChatFrame(this, otherUser);
    }

    private void readLoop() {
        try {
            String line;
            while ((line = client.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Conexion con el servidor perdida", e);
            SwingUtilities.invokeLater(() -> showGlobalError("Se ha perdido la conexion con el servidor."));
        }
    }

    private void handleLine(String line) {
        ProtocolMessage message;
        try {
            message = ProtocolMessage.parse(line);
        } catch (ProtocolException e) {
            LOGGER.log(Level.WARNING, "Linea de protocolo no reconocida: " + line, e);
            return;
        }
        SwingUtilities.invokeLater(() -> dispatch(message));
    }

    private void dispatch(ProtocolMessage message) {
        switch (message.getCommand()) {
            case OK -> handleOk(message);
            case ERROR -> handleError(message);
            case ROOM_LIST -> handleRoomList(message);
            case ROOM_MSG_EVENT -> handleRoomMsgEvent(message);
            case USER_JOINED -> handleUserJoined(message);
            case USER_LEFT -> handleUserLeft(message);
            case PRIVATE_MSG_EVENT -> handlePrivateMsgEvent(message);
            case PRIVATE_CLOSED -> handlePrivateClosed(message);
            case SERVER_SHUTDOWN_NOTICE -> showGlobalError("Aviso del servidor: " + message.arg(0));
            case END_HISTORY, HEARTBEAT_ACK -> {
                // nada que mostrar
            }
            default -> LOGGER.warning("Comando inesperado del servidor: " + message.getCommand());
        }
    }

    private void handleOk(ProtocolMessage message) {
        String context = message.arg(0);
        if (("REGISTER".equals(context) || "LOGIN".equals(context)) && pendingAuthCallback != null) {
            BiConsumer<Boolean, String> callback = pendingAuthCallback;
            pendingAuthCallback = null;
            if ("LOGIN".equals(context)) {
                this.username = message.arg(1);
                callback.accept(true, null);
            } else {
                callback.accept(true, message.arg(2));
            }
        }
    }

    private void handleError(ProtocolMessage message) {
        String code = message.arg(0);
        String detail = message.argCount() > 1 ? message.arg(1) : code;

        boolean isAuthError = code.equals("INVALID_USERNAME") || code.equals("USERNAME_TAKEN")
                || code.equals("INVALID_CREDENTIALS");
        if (isAuthError && pendingAuthCallback != null) {
            BiConsumer<Boolean, String> callback = pendingAuthCallback;
            pendingAuthCallback = null;
            callback.accept(false, detail);
            return;
        }
        showGlobalError(code + ": " + detail);
    }

    private void handleRoomList(ProtocolMessage message) {
        if (roomListFrame != null) {
            roomListFrame.updateRooms(message.argCount() > 0 ? message.arg(0) : "");
        }
    }

    private void handleRoomMsgEvent(ProtocolMessage message) {
        RoomFrame frame = openRooms.get(message.arg(0));
        if (frame != null) {
            frame.appendMessage(message.arg(1), message.arg(2), message.arg(3));
        }
    }

    private void handleUserJoined(ProtocolMessage message) {
        RoomFrame frame = openRooms.get(message.arg(0));
        if (frame != null) {
            frame.appendNotice(message.arg(1) + " se ha unido al salon");
        }
    }

    private void handleUserLeft(ProtocolMessage message) {
        RoomFrame frame = openRooms.get(message.arg(0));
        if (frame != null) {
            frame.appendNotice(message.arg(1) + " ha abandonado el salon");
        }
    }

    private void handlePrivateMsgEvent(ProtocolMessage message) {
        String from = message.arg(0);
        PrivateChatFrame frame = openPrivateChats.computeIfAbsent(from, this::createPrivateChatFrame);
        frame.appendMessage(from, message.arg(1), message.arg(2));
        frame.setVisible(true);
        frame.toFront();
    }

    private void handlePrivateClosed(ProtocolMessage message) {
        PrivateChatFrame frame = openPrivateChats.remove(message.arg(0));
        if (frame != null) {
            frame.notifyClosedByPeer();
        }
    }

    private void showGlobalError(String message) {
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        JOptionPane.showMessageDialog(active, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
