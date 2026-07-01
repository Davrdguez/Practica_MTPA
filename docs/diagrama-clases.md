# Diagrama de clases — Práctica MTPA

Diagrama de las clases más relevantes de la aplicación (servidor, cliente y protocolo
compartido). Se omiten las excepciones de dominio, los getters triviales y las ventanas
Swing menos relevantes para centrar el diagrama en la arquitectura. GitHub renderiza
este bloque Mermaid automáticamente al ver el fichero.

```mermaid
classDiagram
    %% ---------- Servidor ----------
    class ChatServer {
        -UserRegistry userRegistry
        -RoomManager roomManager
        -PersistenceManager persistence
        -Map~String,ClientHandler~ onlineUsers
        -boolean acceptingClients
        -boolean messagingPaused
        +bind()
        +acceptLoop()
        +registerOnline(username, handler)
        +unregisterOnline(username)
        +setAcceptingClients(boolean)
        +setMessagingPaused(boolean)
        +disconnectStaleClients(timeoutMillis)
    }

    class ClientHandler {
        -Socket socket
        -ChatServer server
        -String loggedInUsername
        -Set~Room~ joinedRooms
        +run()
        +onMessage(ChatMessage)
        +onUserJoined(room, username)
        +onUserLeft(room, username)
    }

    class AdminConsole {
        -ChatServer server
        +run()
        +handleCommand(command) boolean
    }

    class UserRegistry {
        -Map~String,User~ usersByUsername
        -AtomicLong nextAccessKey
        +register(username) User
        +login(username, accessKey) User
        +restore(User)
    }

    class User {
        -String username
        -long accessKey
    }

    class RoomManager {
        -Map~String,Room~ rooms
        +getRoom(name) Room
        +allRooms() Collection~Room~
    }

    class Room {
        -List~ChatMessage~ messages
        -Set~RoomListener~ listeners
        -Set~String~ activeUsernames
        +join(username, listener)
        +leave(username, listener)
        +post(username, content) ChatMessage
        +lastDayMessages() List~ChatMessage~
        +messagesBefore(date) List~ChatMessage~
    }

    class ChatMessage {
        -String room
        -String username
        -LocalDateTime timestamp
        -String content
    }

    class RoomListener {
        <<interface>>
        +onMessage(ChatMessage)
        +onUserJoined(room, username)
        +onUserLeft(room, username)
    }

    class PersistenceManager {
        -UserFileStore userFileStore
        -Map~String,RoomFileStore~ roomFileStores
        +loadInto(userRegistry, roomManager)
    }

    class UserFileStore {
        +loadAll() List~User~
        +append(User)
    }

    class RoomFileStore {
        +loadAll(roomName) List~ChatMessage~
        +append(ChatMessage)
    }

    class PersistingRoomListener {
        -RoomFileStore store
    }

    %% ---------- Protocolo compartido ----------
    class Command {
        <<enumeration>>
        REGISTER
        LOGIN
        LOGOUT
        JOIN_ROOM
        ROOM_MSG
        PRIVATE_MSG
        HEARTBEAT
        ...
    }

    class ProtocolMessage {
        -Command command
        -List~String~ args
        +parse(line) ProtocolMessage
        +serialize() String
        +arg(index) String
    }

    %% ---------- Cliente ----------
    class ChatClient {
        -Socket socket
        +connect()
        +send(line)
        +readLine() String
    }

    class HeartbeatSender {
        -ChatClient client
        +start(intervalSeconds)
        +stop()
    }

    class ClientSession {
        -ChatClient client
        -Map~String,RoomFrame~ openRooms
        -Map~String,PrivateChatFrame~ openPrivateChats
        +register(username, callback)
        +login(username, key, callback)
        +joinRoom(room, frame)
        +sendRoomMessage(room, content)
        +sendPrivateMessage(targetUser, content)
    }

    class LoginFrame
    class RoomListFrame
    class RoomFrame
    class PrivateChatFrame

    ChatServer "1" *-- "1" UserRegistry
    ChatServer "1" *-- "1" RoomManager
    ChatServer "1" *-- "1" PersistenceManager
    ChatServer "1" o-- "*" ClientHandler : onlineUsers
    ClientHandler ..|> RoomListener
    ClientHandler --> ChatServer
    AdminConsole --> ChatServer
    UserRegistry "1" o-- "*" User
    RoomManager "1" *-- "*" Room
    Room "1" o-- "*" ChatMessage
    Room "1" o-- "*" RoomListener : listeners
    PersistenceManager --> UserFileStore
    PersistenceManager --> RoomFileStore
    PersistingRoomListener ..|> RoomListener
    PersistingRoomListener --> RoomFileStore
    ClientHandler --> ProtocolMessage
    ProtocolMessage --> Command
    ClientSession *-- ChatClient
    ClientSession *-- HeartbeatSender
    ClientSession --> ProtocolMessage
    LoginFrame --> ClientSession
    RoomListFrame --> ClientSession
    RoomFrame --> ClientSession
    PrivateChatFrame --> ClientSession
```

## Notas sobre las relaciones clave

- `ChatServer` es el punto central: compone `UserRegistry`, `RoomManager` y
  `PersistenceManager`, y mantiene el mapa de usuarios online para poder enrutar
  mensajes privados y estadísticas de administración.
- `ClientHandler` implementa `RoomListener` para poder recibir en tiempo real los
  eventos de los salones a los que está suscrito (mensajes nuevos, entradas y
  salidas de otros usuarios) y reenviarlos al socket del cliente.
- `PersistingRoomListener` también implementa `RoomListener`: se suscribe a cada
  `Room` únicamente para volcar los mensajes nuevos a disco, sin contar como un
  usuario activo del salón.
- `ProtocolMessage` + `Command` son el núcleo del protocolo de aplicación (Capa 7)
  descrito en `protocolo.md`; tanto `ClientHandler` (servidor) como `ClientSession`
  (cliente GUI) dependen de ellos para serializar/parsear cada línea del protocolo.
