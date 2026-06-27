package com.mtpa.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Envia HEARTBEAT periodicamente al servidor para indicar que el cliente
 * sigue conectado y funcionando, mientras la conexion este activa.
 */
public class HeartbeatSender {

    private final ChatClient client;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "heartbeat-sender");
        thread.setDaemon(true);
        return thread;
    });

    public HeartbeatSender(ChatClient client) {
        this.client = client;
    }

    public void start(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> client.send("HEARTBEAT"), 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
