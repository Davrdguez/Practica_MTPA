package com.mtpa.common;

/**
 * Comandos del protocolo de aplicacion (Capa 7) usado entre cliente y servidor.
 */
public enum Command {

    // Cliente -> Servidor
    REGISTER,
    LOGIN,
    LOGOUT,
    LIST_ROOMS,
    JOIN_ROOM,
    LEAVE_ROOM,
    ROOM_MSG,
    HISTORY_REQUEST,
    PRIVATE_MSG,
    PRIVATE_CLOSE,
    HEARTBEAT,

    // Servidor -> Cliente
    OK,
    ERROR,
    ROOM_LIST,
    ROOM_MSG_EVENT,
    END_HISTORY,
    PRIVATE_MSG_EVENT,
    PRIVATE_CLOSED,
    USER_JOINED,
    USER_LEFT,
    HEARTBEAT_ACK,
    SERVER_SHUTDOWN_NOTICE
}
