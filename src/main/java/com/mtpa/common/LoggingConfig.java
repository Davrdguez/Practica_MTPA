package com.mtpa.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configura los logs de la aplicacion (consola + fichero) usando unicamente
 * {@code java.util.logging}, para cumplir el requisito de observabilidad sin
 * añadir dependencias externas.
 */
public final class LoggingConfig {

    private LoggingConfig() {
    }

    public static void configure(String logFileName) {
        try {
            Path logsDir = Path.of("logs");
            Files.createDirectories(logsDir);

            FileHandler fileHandler = new FileHandler(logsDir.resolve(logFileName).toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
        } catch (IOException e) {
            Logger.getLogger(LoggingConfig.class.getName())
                    .log(Level.SEVERE, "No se pudo configurar el log en fichero", e);
        }
    }
}
