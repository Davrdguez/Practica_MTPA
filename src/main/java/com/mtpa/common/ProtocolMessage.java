package com.mtpa.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Representa una linea del protocolo de aplicacion: {@code COMANDO|arg1|arg2|...|argN}.
 *
 * <p>Para los comandos que transportan texto libre (mensajes de usuario, motivos de error...)
 * ese campo es siempre el ultimo de la linea, de forma que puede contener el caracter
 * delimitador '|' sin romper el parseo. El salto de linea esta reservado para separar
 * mensajes y no puede aparecer dentro de un campo.</p>
 */
public final class ProtocolMessage {

    private static final String DELIMITER = "\\|";

    /** Numero total de campos (comando + argumentos) para comandos con texto libre al final. */
    private static final Map<Command, Integer> FIELD_LIMITS = Map.of(
            Command.ROOM_MSG, 3,
            Command.ROOM_MSG_EVENT, 5,
            Command.PRIVATE_MSG, 3,
            Command.PRIVATE_MSG_EVENT, 4,
            Command.ERROR, 3,
            Command.SERVER_SHUTDOWN_NOTICE, 2
    );

    private final Command command;
    private final List<String> args;

    private ProtocolMessage(Command command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    public static ProtocolMessage of(Command command, String... args) {
        return new ProtocolMessage(command, Arrays.asList(args));
    }

    public static ProtocolMessage parse(String line) {
        if (line == null || line.isEmpty()) {
            throw new ProtocolException("Linea vacia");
        }

        int separatorIndex = line.indexOf('|');
        String commandToken = separatorIndex == -1 ? line : line.substring(0, separatorIndex);

        Command command;
        try {
            command = Command.valueOf(commandToken);
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Comando desconocido: " + commandToken);
        }

        int limit = FIELD_LIMITS.getOrDefault(command, -1);
        String[] parts = line.split(DELIMITER, limit);
        List<String> args = parts.length > 1
                ? Arrays.asList(parts).subList(1, parts.length)
                : Collections.emptyList();

        return new ProtocolMessage(command, args);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder(command.name());
        for (String arg : args) {
            sb.append('|').append(arg);
        }
        return sb.toString();
    }

    public Command getCommand() {
        return command;
    }

    public String arg(int index) {
        return args.get(index);
    }

    public int argCount() {
        return args.size();
    }
}
