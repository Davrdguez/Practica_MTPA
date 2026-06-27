package com.mtpa.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ClientMain {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        ChatClient client = new ChatClient(host, port);
        client.connect();
        System.out.println("Conectado a " + host + ":" + port + ". Escribe lineas de protocolo (ej: HEARTBEAT) y pulsa Enter.");

        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = client.readLine()) != null) {
                    System.out.println("<- " + line);
                }
            } catch (IOException e) {
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

        // Margen para que lleguen las respuestas pendientes antes de cerrar la conexion.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        client.close();
    }
}
