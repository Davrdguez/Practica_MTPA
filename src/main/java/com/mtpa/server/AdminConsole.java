package com.mtpa.server;

import com.mtpa.server.room.Room;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Consola de administracion del servidor: lee comandos de la entrada estandar
 * mientras el servidor atiende clientes en otro hilo (ver Hito de Implementacion de Servidor).
 */
public class AdminConsole implements Runnable {

    private final ChatServer server;
    private final BufferedReader input;

    public AdminConsole(ChatServer server) {
        this(server, new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
    }

    public AdminConsole(ChatServer server, BufferedReader input) {
        this.server = server;
        this.input = input;
    }

    @Override
    public void run() {
        printHelp();
        try {
            String line;
            while ((line = input.readLine()) != null) {
                if (!handleCommand(line.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo la consola: " + e.getMessage());
        }
    }

    /** @return false si el comando implica terminar la consola de administracion. */
    boolean handleCommand(String command) {
        switch (command.toLowerCase()) {
            case "" -> { /* linea vacia, no hacer nada */ }
            case "ayuda" -> printHelp();
            case "parar-altas" -> {
                server.setAcceptingClients(false);
                System.out.println("El servidor ha dejado de aceptar nuevos clientes.");
            }
            case "reanudar-altas" -> {
                server.setAcceptingClients(true);
                System.out.println("El servidor vuelve a aceptar nuevos clientes.");
            }
            case "pausar" -> {
                server.setMessagingPaused(true);
                System.out.println("Mensajeria pausada (modo mantenimiento).");
            }
            case "reanudar" -> {
                server.setMessagingPaused(false);
                System.out.println("Mensajeria reanudada.");
            }
            case "stats" -> printStats();
            case "salir" -> {
                System.out.println("Cerrando el servidor...");
                closeServerQuietly();
                return false;
            }
            default -> System.out.println("Comando no reconocido: '" + command + "' (escribe 'ayuda')");
        }
        return true;
    }

    private void printHelp() {
        System.out.println("""
                Comandos disponibles:
                  ayuda           Muestra esta ayuda
                  parar-altas     Deja de aceptar nuevos clientes
                  reanudar-altas  Vuelve a aceptar nuevos clientes
                  pausar          Pausa la mensajeria entre clientes (mantenimiento)
                  reanudar        Reanuda la mensajeria
                  stats           Muestra estadisticas del servidor
                  salir           Cierra el servidor""");
    }

    private void printStats() {
        System.out.println("Usuarios conectados: " + server.onlineUserCount());
        System.out.println("Aceptando nuevos clientes: " + server.isAcceptingClients());
        System.out.println("Mensajeria pausada: " + server.isMessagingPaused());
        for (Room room : server.getRoomManager().allRooms()) {
            System.out.println("  Salon " + room.getName() + ": " + room.activeUserCount()
                    + " usuarios activos, " + room.messageCount() + " mensajes enviados");
        }
    }

    private void closeServerQuietly() {
        try {
            server.stop();
        } catch (IOException e) {
            System.out.println("Error cerrando el servidor: " + e.getMessage());
        }
    }
}
