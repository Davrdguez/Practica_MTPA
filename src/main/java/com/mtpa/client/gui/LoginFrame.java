package com.mtpa.client.gui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

/**
 * Ventana de acceso: registro de usuario nuevo o login con usuario+clave.
 */
public class LoginFrame extends JFrame {

    private final JTextField hostField = new JTextField("localhost", 10);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField usernameField = new JTextField(15);
    private final JTextField keyField = new JTextField(15);

    private ClientSession session;

    public LoginFrame() {
        super("MTPA Chat - Acceso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(panel, c, row++, "Servidor:", hostField);
        addRow(panel, c, row++, "Puerto:", portField);
        addRow(panel, c, row++, "Usuario:", usernameField);
        addRow(panel, c, row++, "Clave:", keyField);

        JButton registerButton = new JButton("Registrarse");
        JButton loginButton = new JButton("Entrar");
        registerButton.addActionListener(e -> onRegister());
        loginButton.addActionListener(e -> onLogin());

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(loginButton);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        panel.add(buttons, c);

        getContentPane().add(panel);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        panel.add(field, c);
    }

    private ClientSession ensureSession() throws IOException {
        if (session == null) {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            session = new ClientSession(host, port);
        }
        return session;
    }

    private void onRegister() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe un nombre de usuario.",
                    "Falta el usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ClientSession activeSession = ensureSession();
            activeSession.register(username, (success, detail) -> {
                if (success) {
                    keyField.setText(detail);
                    JOptionPane.showMessageDialog(this,
                            "Usuario registrado. Tu clave de acceso es: " + detail + "\nGuardala, la necesitas para entrar.",
                            "Registro correcto", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo registrar: " + detail,
                            "Error de registro", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + e.getMessage(),
                    "Error de conexion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String key = keyField.getText().trim();
        if (username.isEmpty() || key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario y clave son obligatorios.",
                    "Faltan datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ClientSession activeSession = ensureSession();
            activeSession.login(username, key, (success, detail) -> {
                if (success) {
                    openRoomList(activeSession);
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo iniciar sesion: " + detail,
                            "Error de login", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + e.getMessage(),
                    "Error de conexion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRoomList(ClientSession activeSession) {
        activeSession.startHeartbeat();
        RoomListFrame roomListFrame = new RoomListFrame(activeSession);
        activeSession.setRoomListFrame(roomListFrame);
        roomListFrame.setVisible(true);
        activeSession.refreshRoomList();
        dispose();
    }
}
