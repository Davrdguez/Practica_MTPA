package com.mtpa.client.gui;

import com.mtpa.common.LoggingConfig;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada del cliente grafico: abre la ventana de acceso ({@link LoginFrame})
 * en el Event Dispatch Thread de Swing.
 */
public class GuiClientMain {

    public static void main(String[] args) {
        LoggingConfig.configure("client.log");
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
