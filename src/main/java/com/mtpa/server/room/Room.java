package com.mtpa.server.room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Salon de chat: guarda el historial de mensajes y reenvia en vivo a los
 * clientes suscritos (los que han hecho JOIN_ROOM y siguen conectados).
 */
public class Room {

    public static final int MAX_MESSAGE_LENGTH = 190;

    private final String name;
    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private final Set<RoomListener> listeners = new CopyOnWriteArraySet<>();
    private final Set<String> activeUsernames = new CopyOnWriteArraySet<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Suscribe un oyente permanente (p.ej. persistencia) sin contarlo como usuario activo. */
    public void addListener(RoomListener listener) {
        listeners.add(listener);
    }

    public void join(String username, RoomListener listener) {
        activeUsernames.add(username);
        listeners.add(listener);
        for (RoomListener l : listeners) {
            l.onUserJoined(name, username);
        }
    }

    public void leave(String username, RoomListener listener) {
        activeUsernames.remove(username);
        listeners.remove(listener);
        for (RoomListener l : listeners) {
            l.onUserLeft(name, username);
        }
    }

    public ChatMessage post(String username, String content) {
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException(content.length());
        }

        ChatMessage message = new ChatMessage(name, username, LocalDateTime.now(), content);
        messages.add(message);
        for (RoomListener listener : listeners) {
            listener.onMessage(message);
        }
        return message;
    }

    /** Repone mensajes ya existentes (p.ej. cargados de persistencia) sin volver a notificarlos. */
    public void restoreHistory(List<ChatMessage> historicalMessages) {
        messages.addAll(historicalMessages);
    }

    public List<ChatMessage> lastDayMessages() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return messages.stream()
                .filter(m -> m.getTimestamp().isAfter(since))
                .toList();
    }

    /** Mensajes del dia anterior a {@code referenceDate} (para "cargar mensajes de dias anteriores"). */
    public List<ChatMessage> messagesBefore(LocalDate referenceDate) {
        LocalDateTime endExclusive = referenceDate.atStartOfDay();
        LocalDateTime startInclusive = referenceDate.minusDays(1).atStartOfDay();
        return messages.stream()
                .filter(m -> !m.getTimestamp().isBefore(startInclusive) && m.getTimestamp().isBefore(endExclusive))
                .toList();
    }

    public int activeUserCount() {
        return activeUsernames.size();
    }

    public int messageCount() {
        return messages.size();
    }
}
