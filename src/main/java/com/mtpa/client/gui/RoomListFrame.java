package com.mtpa.client.gui;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Ventana principal tras el login: lista de salones del servidor, con acceso
 * a entrar a un salon o abrir un chat privado.
 */
public class RoomListFrame extends JFrame {

    private final ClientSession session;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> roomJList = new JList<>(listModel);

    public RoomListFrame(ClientSession session) {
        super("MTPA Chat - " + session.getUsername());
        this.session = session;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUi();
        setSize(320, 400);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JButton openButton = new JButton("Entrar al salon");
        JButton refreshButton = new JButton("Actualizar");
        JButton privateButton = new JButton("Mensaje privado...");

        openButton.addActionListener(e -> openSelectedRoom());
        refreshButton.addActionListener(e -> session.refreshRoomList());
        privateButton.addActionListener(e -> openPrivateChatDialog());

        roomJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedRoom();
                }
            }
        });

        JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 4));
        buttons.add(openButton);
        buttons.add(refreshButton);
        buttons.add(privateButton);

        setLayout(new BorderLayout(8, 8));
        add(new JLabel("Salones disponibles (doble clic para entrar):"), BorderLayout.NORTH);
        add(new JScrollPane(roomJList), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    public void updateRooms(String summary) {
        listModel.clear();
        if (summary == null || summary.isBlank()) {
            return;
        }
        for (String entry : summary.split(",")) {
            listModel.addElement(entry);
        }
    }

    private void openSelectedRoom() {
        String selected = roomJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un salon de la lista.",
                    "Ningun salon seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String roomName = selected.split(":")[0];
        RoomFrame roomFrame = new RoomFrame(session, roomName);
        roomFrame.setVisible(true);
        session.joinRoom(roomName, roomFrame);
    }

    private void openPrivateChatDialog() {
        String target = JOptionPane.showInputDialog(this, "Usuario con el que quieres chatear en privado:");
        if (target != null && !target.isBlank()) {
            session.openOrFocusPrivateChat(target.trim());
        }
    }
}
