package com.mtpa.client.gui;

import com.mtpa.common.LoggingConfig;

import javax.swing.SwingUtilities;

public class GuiClientMain {

    public static void main(String[] args) {
        LoggingConfig.configure("client.log");
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
