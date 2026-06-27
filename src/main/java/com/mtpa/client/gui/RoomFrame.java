package com.mtpa.client.gui;

import com.mtpa.common.Protocol;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Ventana de un salon: transcripcion de mensajes, envio (limitado a 190 caracteres)
 * y carga de mensajes de dias anteriores.
 */
public class RoomFrame extends JFrame {

    private final ClientSession session;
    private final String roomName;
    private final JTextArea transcript = new JTextArea();
    private final JTextField inputField = new JTextField();
    private LocalDate nextHistoryBoundary = LocalDate.now();

    public RoomFrame(ClientSession session, String roomName) {
        super("Salon: " + roomName);
        this.session = session;
        this.roomName = roomName;
        buildUi();
        setSize(480, 420);
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                session.leaveRoom(roomName);
                dispose();
            }
        });
    }

    private void buildUi() {
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);

        JButton historyButton = new JButton("Cargar mensajes anteriores");
        historyButton.addActionListener(this::onLoadOlderHistory);

        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(this::onSend);
        inputField.addActionListener(this::onSend);
        applyMaxLength(inputField, Protocol.MAX_ROOM_MESSAGE_LENGTH);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(inputField, BorderLayout.CENTER);
        south.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout(4, 4));
        add(historyButton, BorderLayout.NORTH);
        add(new JScrollPane(transcript), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void onSend(ActionEvent event) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        session.sendRoomMessage(roomName, text);
        inputField.setText("");
    }

    private void onLoadOlderHistory(ActionEvent event) {
        appendNotice("cargando mensajes anteriores a " + nextHistoryBoundary);
        session.requestHistory(roomName, nextHistoryBoundary);
        nextHistoryBoundary = nextHistoryBoundary.minusDays(1);
    }

    public void appendMessage(String user, String timestamp, String content) {
        transcript.append(formatTimestamp(timestamp) + " " + user + ": " + content + "\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    public void appendNotice(String text) {
        transcript.append("*** " + text + " ***\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    private String formatTimestamp(String isoTimestamp) {
        try {
            return "[" + LocalDateTime.parse(isoTimestamp).toLocalTime().withNano(0) + "]";
        } catch (DateTimeParseException e) {
            return "[" + isoTimestamp + "]";
        }
    }

    private void applyMaxLength(JTextField field, int maxLength) {
        ((PlainDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (fb.getDocument().getLength() + string.length() <= maxLength) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                int resultingLength = fb.getDocument().getLength() - length + (text == null ? 0 : text.length());
                if (resultingLength <= maxLength) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }
}
