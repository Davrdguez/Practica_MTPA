package com.mtpa.client.gui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Ventana de conversacion privada con un usuario. Desaparece (sin persistir nada)
 * en cuanto cualquiera de las dos partes cierra su ventana.
 */
public class PrivateChatFrame extends JFrame {

    private final ClientSession session;
    private final String otherUser;
    private final JTextArea transcript = new JTextArea();
    private final JTextField inputField = new JTextField();
    private boolean closedByPeer;

    public PrivateChatFrame(ClientSession session, String otherUser) {
        super("Privado con " + otherUser);
        this.session = session;
        this.otherUser = otherUser;
        buildUi();
        setSize(380, 360);
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!closedByPeer) {
                    session.closePrivateChat(otherUser);
                }
                dispose();
            }
        });
    }

    private void buildUi() {
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);

        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(this::onSend);
        inputField.addActionListener(this::onSend);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(inputField, BorderLayout.CENTER);
        south.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout(4, 4));
        add(new JScrollPane(transcript), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void onSend(ActionEvent event) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        session.sendPrivateMessage(otherUser, text);
        appendMessage(session.getUsername(), LocalDateTime.now().toString(), text);
        inputField.setText("");
    }

    public void appendMessage(String user, String timestamp, String content) {
        transcript.append(formatTimestamp(timestamp) + " " + user + ": " + content + "\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    public void notifyClosedByPeer() {
        closedByPeer = true;
        JOptionPane.showMessageDialog(this, otherUser + " ha cerrado la conversacion.",
                "Conversacion cerrada", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private String formatTimestamp(String isoTimestamp) {
        try {
            return "[" + LocalDateTime.parse(isoTimestamp).toLocalTime().withNano(0) + "]";
        } catch (DateTimeParseException e) {
            return "[" + isoTimestamp + "]";
        }
    }
}
