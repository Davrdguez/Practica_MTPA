package com.mtpa.server;

import com.mtpa.common.LoggingConfig;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Punto de entrada del servidor: arranca el {@link ChatServer} en un hilo de aceptacion
 * y deja el hilo principal atendiendo la {@link AdminConsole} en modo consola.
 */
public class ServerMain {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        LoggingConfig.configure("server.log");
        Logger logger = Logger.getLogger(ServerMain.class.getName());
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        ChatServer server = new ChatServer(port);
        try {
            server.bind();
        } catch (IOException e) {
            logger.severe("No se pudo iniciar el servidor: " + e.getMessage());
            return;
        }

        Thread acceptThread = new Thread(server::acceptLoop, "accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();

        new AdminConsole(server).run();
    }
}
