package com.mtpa.server;

import java.io.IOException;
import java.util.logging.Logger;

public class ServerMain {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(ServerMain.class.getName());
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        ChatServer server = new ChatServer(port);
        try {
            server.start();
        } catch (IOException e) {
            logger.severe("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}
