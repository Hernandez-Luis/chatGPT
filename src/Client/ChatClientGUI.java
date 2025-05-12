package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ChatClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter output;
    private BufferedReader input;
    private String userName;

    public ChatClientGUI(String userName) {
        this.userName = userName;
        createUI();
        connectToServer();
        startMessageReader();
    }

    private void createUI() {
        setTitle("Chat de " + userName);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBackground(Color.BLACK);
        chatArea.setForeground(Color.GREEN);

        inputField = new JTextField();
        sendButton = new JButton("Enviar");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 9090);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println(userName + " se ha unido al chat.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor.");
            System.exit(1);
        }
    }

    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    chatArea.append(line + "\n");
                }
            } catch (IOException e) {
                chatArea.append("Error de conexión...\n");
            }
        });
        readerThread.start();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            output.println(userName + ": " + text);
            inputField.setText("");
        }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Ingresa tu nombre:");
        if (name != null && !name.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClientGUI(name));
        }
    }
}
