# Protocolo de Aplicación (Capa 7) — Práctica MTPA

## 1. Transporte y framing

- Transporte: TCP sobre `Socket` / `ServerSocket` (java.net), sin frameworks de alto nivel.
- Codificación: UTF-8.
- Framing: un mensaje por línea, terminada en `\n`. Se lee con `BufferedReader.readLine()` y se escribe con `PrintWriter.println()` (autoflush activado).
- El contenido de un mensaje (texto de chat) **no puede contener saltos de línea**; el cliente debe sanearlo (reemplazar `\r`/`\n` por espacio) antes de enviarlo.

## 2. Formato general

```
COMANDO|arg1|arg2|...|argN
```

- `COMANDO` es uno de los valores del enum `Command` (ver tabla más abajo).
- Los argumentos van separados por `|`.
- **Regla del último campo libre**: en los comandos que transportan texto libre (mensaje de chat, motivo de error, etc.) ese campo es siempre el último de la línea. Esto permite que el contenido contenga el carácter `|` sin romper el parseo, porque el parser sólo divide la línea en el número de campos esperado para ese comando (`String.split(regex, limit)`), y el último trozo se queda con el resto de la línea íntegro.
- Los campos que no son texto libre (usuario, sala, fechas, claves) están restringidos a caracteres alfanuméricos y no pueden contener `|`, lo cual se valida en el registro/alta.

Implementación de referencia: `com.mtpa.common.Command` y `com.mtpa.common.ProtocolMessage` (con sus tests en `ProtocolMessageTest`).

## 3. Ciclo de vida de una conexión

1. El cliente abre el `Socket` contra el servidor.
2. `REGISTER` (una vez, la primera vez que se usa ese nombre) o `LOGIN` en cada conexión posterior.
3. Tras el login correcto, el servidor envía `ROOM_LIST` con los salones disponibles.
4. El cliente hace `JOIN_ROOM` por cada salón que abre; el servidor responde con el historial del último día (`ROOM_MSG_EVENT`* + `END_HISTORY`).
5. Mientras la sesión está activa: envío/recepción de `ROOM_MSG` / `ROOM_MSG_EVENT`, `PRIVATE_MSG` / `PRIVATE_MSG_EVENT`, `HEARTBEAT` periódico.
6. `LOGOUT` o cierre del socket termina la sesión; el servidor libera al usuario de todos los salones y notifica `USER_LEFT`.

## 4. Comandos Cliente → Servidor

| Comando | Argumentos | Descripción |
|---|---|---|
| `REGISTER` | `username` | Da de alta un usuario nuevo. El servidor responde `OK\|REGISTER\|username\|clave` con la clave autonumérica generada, o `ERROR` si el nombre ya existe. |
| `LOGIN` | `username`, `clave` | Inicia sesión. Responde `OK\|LOGIN\|username` + `ROOM_LIST`, o `ERROR\|INVALID_CREDENTIALS`. |
| `LOGOUT` | — | Cierra sesión de forma ordenada: abandona todos los salones (notificando `USER_LEFT`) y dejar de estar online, sin cerrar el socket. Responde `OK\|LOGOUT`. |
| `LIST_ROOMS` | — | Pide la lista de salones. Responde `ROOM_LIST`. |
| `JOIN_ROOM` | `sala` | Se une a un salón. Responde con el historial del último día y notifica `USER_JOINED` al resto de miembros. |
| `LEAVE_ROOM` | `sala` | Abandona un salón. Responde `OK\|LEAVE_ROOM\|sala` y notifica `USER_LEFT` al resto. |
| `ROOM_MSG` | `sala`, `contenido` | Envía un mensaje al salón (máx. 190 caracteres). Se reenvía como `ROOM_MSG_EVENT` a todos los miembros conectados. |
| `HISTORY_REQUEST` | `sala`, `antesDe` (fecha `yyyy-MM-dd`) | Pide los mensajes de días anteriores al indicado. Responde con `ROOM_MSG_EVENT`* + `END_HISTORY`. |
| `PRIVATE_MSG` | `destinatario`, `contenido` | Mensaje privado. Sólo se entrega si el destinatario está conectado; si no, `ERROR\|USER_NOT_CONNECTED`. No se persiste. |
| `PRIVATE_CLOSE` | `destinatario` | Avisa de que se ha cerrado la ventana privada; el servidor reenvía `PRIVATE_CLOSED` al otro extremo. |
| `HEARTBEAT` | — | Ping periódico (cada X minutos) para indicar que el cliente sigue activo. Responde `HEARTBEAT_ACK`. |

## 5. Comandos Servidor → Cliente

| Comando | Argumentos | Descripción |
|---|---|---|
| `OK` | `contexto`, `...` | Confirmación genérica de una operación (p.ej. `OK\|REGISTER\|ana\|1042`). |
| `ERROR` | `codigo`, `mensaje` | Error genérico. Códigos: `USERNAME_TAKEN`, `INVALID_CREDENTIALS`, `NOT_LOGGED_IN`, `ROOM_NOT_FOUND`, `MESSAGE_TOO_LONG`, `USER_NOT_CONNECTED`, `UNKNOWN_COMMAND`, `SERVER_PAUSED`. |
| `ROOM_LIST` | `sala1:n1,sala2:n2,...` | Lista de salones con nº de usuarios activos en cada uno. |
| `ROOM_MSG_EVENT` | `sala`, `usuario`, `timestamp` (ISO-8601), `contenido` | Mensaje de salón (en vivo o como parte del historial). |
| `END_HISTORY` | `sala` | Marca el final del lote de historial enviado para esa sala. |
| `PRIVATE_MSG_EVENT` | `remitente`, `timestamp`, `contenido` | Mensaje privado recibido. |
| `PRIVATE_CLOSED` | `usuario` | El otro extremo de la conversación privada ha cerrado la ventana. |
| `USER_JOINED` | `sala`, `usuario` | Notificación: un usuario nuevo ha entrado al salón. |
| `USER_LEFT` | `sala`, `usuario` | Notificación: un usuario ha abandonado el salón. |
| `HEARTBEAT_ACK` | — | Confirmación de heartbeat. |
| `SERVER_SHUTDOWN_NOTICE` | `motivo` | El servidor va a dejar de aceptar mensajes (mantenimiento) o clientes nuevos. |

## 6. Ejemplos de intercambio

**Registro + login:**
```
C: REGISTER|ana
S: OK|REGISTER|ana|1042

C: LOGIN|ana|1042
S: OK|LOGIN|ana
S: ROOM_LIST|ia:3,deportes:1,therian:0,manga:5,uemc:2
```

**Unirse a un salón y recibir historial del último día:**
```
C: JOIN_ROOM|ia
S: ROOM_MSG_EVENT|ia|bob|2026-06-26T18:04:11|bienvenidos al salon
S: ROOM_MSG_EVENT|ia|ana|2026-06-26T19:21:50|hola que tal
S: END_HISTORY|ia
S: USER_JOINED|ia|ana          (broadcast al resto de miembros del salón)
```

**Mensaje a un salón:**
```
C: ROOM_MSG|ia|que opinais del nuevo modelo
S: ROOM_MSG_EVENT|ia|ana|2026-06-27T10:15:02|que opinais del nuevo modelo   (a todos los miembros, incluido el emisor)
```

**Mensaje privado a usuario no conectado:**
```
C: PRIVATE_MSG|carlos|hola
S: ERROR|USER_NOT_CONNECTED|carlos no esta conectado
```

**Heartbeat:**
```
C: HEARTBEAT
S: HEARTBEAT_ACK
```

## 7. Administración del servidor

Los comandos de administración (dejar de aceptar clientes, pausar mensajería, mostrar estadísticas) **no forman parte de este protocolo de red**: se introducen directamente por consola en el proceso del servidor (entrada estándar), ya que el enunciado especifica que el servidor es una aplicación de consola en modo "administración".

El efecto visible para los clientes es siempre reactivo, no proactivo: mientras el operador tiene la mensajería pausada, cualquier intento de `ROOM_MSG` o `PRIVATE_MSG` recibe `ERROR|SERVER_PAUSED|...` en el momento del envío; los clientes ya conectados no reciben ningún aviso adicional en el instante en que se activa la pausa. `SERVER_SHUTDOWN_NOTICE` está reservado en el protocolo para un futuro aviso proactivo, pero el servidor no lo emite todavía.
