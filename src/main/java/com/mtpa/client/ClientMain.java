package com.mtpa.client;

import com.mtpa.common.LoggingConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente de consola: conecta al servidor, envia heartbeats periodicos y permite
 * escribir lineas de protocolo directamente por la entrada estandar.
 */
public class ClientMain {

    private static final Logger LOGGER = Logger.getLogger(ClientMain.class.getName());
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;

    public static void main(String[] args) throws IOException {
        LoggingConfig.configure("client.log");
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        ChatClient client = new ChatClient(host, port);
        client.connect();
        LOGGER.info("Conectado a " + host + ":" + port);
        System.out.println("Conectado a " + host + ":" + port + ". Escribe lineas de protocolo (ej: HEARTBEAT) y pulsa Enter.");

        HeartbeatSender heartbeatSender = new HeartbeatSender(client);
        heartbeatSender.start(HEARTBEAT_INTERVAL_SECONDS);

        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = client.readLine()) != null) {
                    System.out.println("<- " + line);
                }
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "Conexion con el servidor cerrada", e);
                System.out.println("Conexion con el servidor cerrada.");
            }
        }, "server-listener");
        listener.setDaemon(true);
        listener.start();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String userLine;
        while ((userLine = stdIn.readLine()) != null) {
            client.send(userLine);
        }

        heartbeatSender.stop();

        // Margen para que lleguen las respuestas pendientes antes de cerrar la conexion.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        client.close();
    }
}
