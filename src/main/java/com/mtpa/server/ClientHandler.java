package com.mtpa.server;

import com.mtpa.common.Command;
import com.mtpa.common.ProtocolException;
import com.mtpa.common.ProtocolMessage;
import com.mtpa.server.room.ChatMessage;
import com.mtpa.server.room.MessageTooLongException;
import com.mtpa.server.room.Room;
import com.mtpa.server.room.RoomListener;
import com.mtpa.server.room.RoomNotFoundException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Atiende a un cliente conectado: lee lineas del protocolo, despacha hacia
 * UserRegistry/RoomManager, y reenvia los eventos de los salones a los que esta suscrito.
 */
public class ClientHandler implements Runnable, RoomListener {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ChatServer server;
    private final Set<Room> joinedRooms = ConcurrentHashMap.newKeySet();
    private String loggedInUsername;
    private PrintWriter out;
    private volatile long lastActivityMillis = System.currentTimeMillis();

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        LOGGER.info("Cliente conectado: " + socket.getRemoteSocketAddress());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Conexion interrumpida con " + socket.getRemoteSocketAddress(), e);
        } finally {
            leaveAllRooms();
            server.unregisterOnline(loggedInUsername);
            closeQuietly();
            LOGGER.info("Cliente desconectado: " + socket.getRemoteSocketAddress());
        }
    }

    private void handleLine(String line) {
        lastActivityMillis = System.currentTimeMillis();

        ProtocolMessage message;
        try {
            message = ProtocolMessage.parse(line);
        } catch (ProtocolException e) {
            send(ProtocolMessage.of(Command.ERROR, "UNKNOWN_COMMAND", e.getMessage()));
            return;
        }

        try {
            dispatch(message);
        } catch (NotLoggedInException e) {
            send(ProtocolMessage.of(Command.ERROR, "NOT_LOGGED_IN", e.getMessage()));
        } catch (RoomNotFoundException e) {
            send(ProtocolMessage.of(Command.ERROR, "ROOM_NOT_FOUND", e.getMessage()));
        } catch (MessageTooLongException e) {
            send(ProtocolMessage.of(Command.ERROR, "MESSAGE_TOO_LONG", e.getMessage()));
        } catch (UserNotConnectedException e) {
            send(ProtocolMessage.of(Command.ERROR, "USER_NOT_CONNECTED", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Mensaje mal formado de " + socket.getRemoteSocketAddress(), e);
            send(ProtocolMessage.of(Command.ERROR, "MALFORMED_MESSAGE", message.getCommand().name()));
        }
    }

    private void dispatch(ProtocolMessage message) {
        switch (message.getCommand()) {
            case HEARTBEAT -> send(ProtocolMessage.of(Command.HEARTBEAT_ACK));
            case REGISTER -> handleRegister(message);
            case LOGIN -> handleLogin(message);
            case LIST_ROOMS -> handleListRooms();
            case JOIN_ROOM -> handleJoinRoom(message);
            case LEAVE_ROOM -> handleLeaveRoom(message);
            case ROOM_MSG -> handleRoomMsg(message);
            case HISTORY_REQUEST -> handleHistoryRequest(message);
            case PRIVATE_MSG -> handlePrivateMsg(message);
            case PRIVATE_CLOSE -> handlePrivateClose(message);
            default -> send(ProtocolMessage.of(Command.OK, message.getCommand().name()));
        }
    }

    private void handleRegister(ProtocolMessage message) {
        String username = message.arg(0);
        try {
            User user = server.getUserRegistry().register(username);
            server.getPersistence().getUserFileStore().append(user);
            LOGGER.info("Usuario registrado: " + username);
            send(ProtocolMessage.of(Command.OK, "REGISTER", user.getUsername(), String.valueOf(user.getAccessKey())));
        } catch (InvalidUsernameException e) {
            send(ProtocolMessage.of(Command.ERROR, "INVALID_USERNAME", e.getMessage()));
        } catch (UsernameAlreadyExistsException e) {
            send(ProtocolMessage.of(Command.ERROR, "USERNAME_TAKEN", e.getMessage()));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "No se pudo persistir el usuario " + username, e);
            send(ProtocolMessage.of(Command.ERROR, "INTERNAL_ERROR", "No se pudo guardar el usuario"));
        }
    }

    private void handleLogin(ProtocolMessage message) {
        String username = message.arg(0);
        try {
            long accessKey = Long.parseLong(message.arg(1));
            server.getUserRegistry().login(username, accessKey);
            loggedInUsername = username;
            server.registerOnline(username, this);
            LOGGER.info("Login correcto: " + username);
            send(ProtocolMessage.of(Command.OK, "LOGIN", username));
        } catch (NumberFormatException | InvalidCredentialsException e) {
            send(ProtocolMessage.of(Command.ERROR, "INVALID_CREDENTIALS", "Usuario o clave incorrectos"));
        }
    }

    private void handleListRooms() {
        String summary = server.getRoomManager().allRooms().stream()
                .map(room -> room.getName() + ":" + room.activeUserCount())
                .collect(Collectors.joining(","));
        send(ProtocolMessage.of(Command.ROOM_LIST, summary));
    }

    private void handleJoinRoom(ProtocolMessage message) {
        requireLogin();
        Room room = server.getRoomManager().getRoom(message.arg(0));

        room.join(loggedInUsername, this);
        joinedRooms.add(room);
        LOGGER.info(loggedInUsername + " se ha unido al salon " + room.getName());

        for (ChatMessage history : room.lastDayMessages()) {
            send(toRoomMsgEvent(history));
        }
        send(ProtocolMessage.of(Command.END_HISTORY, room.getName()));
    }

    private void handleLeaveRoom(ProtocolMessage message) {
        requireLogin();
        Room room = server.getRoomManager().getRoom(message.arg(0));

        room.leave(loggedInUsername, this);
        joinedRooms.remove(room);
        LOGGER.info(loggedInUsername + " ha abandonado el salon " + room.getName());
        send(ProtocolMessage.of(Command.OK, "LEAVE_ROOM", room.getName()));
    }

    private void handleRoomMsg(ProtocolMessage message) {
        requireLogin();
        Room room = server.getRoomManager().getRoom(message.arg(0));
        room.post(loggedInUsername, message.arg(1));
        LOGGER.info(loggedInUsername + " envio un mensaje en el salon " + room.getName());
    }

    private void handleHistoryRequest(ProtocolMessage message) {
        requireLogin();
        Room room = server.getRoomManager().getRoom(message.arg(0));
        LocalDate beforeDate = LocalDate.parse(message.arg(1));

        for (ChatMessage history : room.messagesBefore(beforeDate)) {
            send(toRoomMsgEvent(history));
        }
        send(ProtocolMessage.of(Command.END_HISTORY, room.getName()));
    }

    private void handlePrivateMsg(ProtocolMessage message) {
        requireLogin();
        String targetUsername = message.arg(0);
        String content = message.arg(1);

        ClientHandler targetHandler = server.findOnline(targetUsername)
                .orElseThrow(() -> new UserNotConnectedException(targetUsername));

        targetHandler.send(ProtocolMessage.of(Command.PRIVATE_MSG_EVENT, loggedInUsername,
                LocalDateTime.now().toString(), content));
        LOGGER.info(loggedInUsername + " envio un mensaje privado a " + targetUsername);
    }

    private void handlePrivateClose(ProtocolMessage message) {
        requireLogin();
        String targetUsername = message.arg(0);

        server.findOnline(targetUsername)
                .ifPresent(handler -> handler.send(ProtocolMessage.of(Command.PRIVATE_CLOSED, loggedInUsername)));
    }

    private void requireLogin() {
        if (loggedInUsername == null) {
            throw new NotLoggedInException();
        }
    }

    private ProtocolMessage toRoomMsgEvent(ChatMessage message) {
        return ProtocolMessage.of(Command.ROOM_MSG_EVENT, message.getRoom(), message.getUsername(),
                message.getTimestamp().toString(), message.getContent());
    }

    @Override
    public void onMessage(ChatMessage message) {
        send(toRoomMsgEvent(message));
    }

    @Override
    public void onUserJoined(String roomName, String username) {
        send(ProtocolMessage.of(Command.USER_JOINED, roomName, username));
    }

    @Override
    public void onUserLeft(String roomName, String username) {
        send(ProtocolMessage.of(Command.USER_LEFT, roomName, username));
    }

    private void send(ProtocolMessage message) {
        if (out != null) {
            out.println(message.serialize());
        }
    }

    private void leaveAllRooms() {
        for (Room room : joinedRooms) {
            room.leave(loggedInUsername, this);
        }
        joinedRooms.clear();
    }

    public long getLastActivityMillis() {
        return lastActivityMillis;
    }

    /** Fuerza el cierre de la conexion (p.ej. por inactividad de heartbeat); el hilo propio hace su limpieza habitual. */
    public void disconnect() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (out != null) {
            out.close();
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cerrando socket", e);
        }
    }
}
