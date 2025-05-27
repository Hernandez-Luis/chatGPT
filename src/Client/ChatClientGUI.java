package Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ChatClientGUI extends JFrame {
    private JTextPane chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter output;
    private BufferedReader input;
    private String userName;
    private Style sentMessageStyle;
    private Style receivedMessageStyle;

    private DataOutputStream dataOut;
    private DataInputStream dataIn;

    public ChatClientGUI(String userName, String serverIP) {
        this.userName = userName;
        createUI();
        connectToServer(serverIP);
        startMessageReader();
    }

    private void createUI() {
        setTitle("Chat de " + userName);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Panel principal con fondo azul claro
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(230, 240, 255));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Área de chat con estilo de burbujas
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(230, 240, 255));

        // Estilos para los mensajes
        StyledDocument doc = chatArea.getStyledDocument();

        // Estilo para mensajes enviados (derecha)
        sentMessageStyle = doc.addStyle("sent", null);
        StyleConstants.setAlignment(sentMessageStyle, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setForeground(sentMessageStyle, Color.WHITE);
        StyleConstants.setBackground(sentMessageStyle, new Color(0, 120, 215));
        StyleConstants.setFontFamily(sentMessageStyle, "Segoe UI");
        StyleConstants.setFontSize(sentMessageStyle, 14);
        StyleConstants.setBold(sentMessageStyle, false);

        // Estilo para mensajes recibidos (izquierda)
        receivedMessageStyle = doc.addStyle("received", null);
        StyleConstants.setAlignment(receivedMessageStyle, StyleConstants.ALIGN_LEFT);
        StyleConstants.setForeground(receivedMessageStyle, Color.BLACK);
        StyleConstants.setBackground(receivedMessageStyle, Color.WHITE);
        StyleConstants.setFontFamily(receivedMessageStyle, "Segoe UI");
        StyleConstants.setFontSize(receivedMessageStyle, 14);
        StyleConstants.setBold(receivedMessageStyle, false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(230, 240, 255));

        // Panel de entrada
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(230, 240, 255));
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        // Botón azul más intenso
        sendButton = new JButton("Enviar");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setBackground(new Color(0, 120, 215));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton imageButton = new JButton("📎 Imagen");
        imageButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        imageButton.setBackground(new Color(0, 120, 215));
        imageButton.setForeground(Color.WHITE);
        imageButton.setFocusPainted(false);
        imageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imageButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        imageButton.addActionListener(e -> sendImage());
        inputPanel.add(imageButton, BorderLayout.WEST);

        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(0, 140, 235));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(new Color(0, 120, 215));
            }
        });

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                output.println("IMAGE:" + file.getName());
                dataOut.writeInt(imageBytes.length);
                dataOut.write(imageBytes);
                dataOut.flush();

                // Mostrar en el área de chat también
                appendSentMessage("Imagen enviada: " + file.getName());
                appendImageMessage(file);
            } catch (IOException e) {
                appendSystemMessage("Error al enviar imagen.");
            }
        }
    }

    private void connectToServer(String serverIP) {
        try {
            Socket socket = new Socket(serverIP, 9090);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            dataOut = new DataOutputStream(socket.getOutputStream());
            dataIn = new DataInputStream(socket.getInputStream());

            output.println("JOIN:" + userName);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar al servidor en " + serverIP + "\n" +
                            "Asegúrate que:\n" +
                            "1. El servidor esté ejecutándose\n" +
                            "2. La dirección IP sea correcta\n" +
                            "3. El firewall permita conexiones en el puerto 9090",
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.startsWith("IMAGE:")) {
                        String fileName = line.substring(6);
                        int length = dataIn.readInt();
                        byte[] imageData = new byte[length];
                        dataIn.readFully(imageData);

                        // Guardar la imagen
                        File imgFile = new File("received_" + fileName);
                        try (FileOutputStream fos = new FileOutputStream(imgFile)) {
                            fos.write(imageData);
                        }

                        SwingUtilities.invokeLater(() -> appendImageMessage(imgFile));
                    } else {
                        final String message = line;
                        SwingUtilities.invokeLater(() -> appendReceivedMessage(message));
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> appendSystemMessage("Error de conexión con el servidor..."));
            }
        });
        readerThread.start();
    }

    private void appendImageMessage(File imageFile) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            chatArea.setCaretPosition(doc.getLength());
            chatArea.insertIcon(new ImageIcon(imageFile.getAbsolutePath()));
            doc.insertString(doc.getLength(), "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            // Mostrar mensaje enviado localmente
            appendSentMessage("[" + userName + "] :" + text);

            // Enviar mensaje al servidor
            output.println(text);
            inputField.setText("");
        }
    }

    private void appendSentMessage(String message) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            doc.insertString(doc.getLength(), message + "\n", sentMessageStyle);
            chatArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendReceivedMessage(String message) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            doc.insertString(doc.getLength(), message + "\n", receivedMessageStyle);
            chatArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendSystemMessage(String message) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            Style tempStyle = doc.addStyle("temp", null);
            StyleConstants.setAlignment(tempStyle, StyleConstants.ALIGN_CENTER);
            StyleConstants.setForeground(tempStyle, new Color(100, 100, 100));
            StyleConstants.setItalic(tempStyle, true);
            doc.insertString(doc.getLength(), message + "\n", tempStyle);
            chatArea.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Ventana de inicio mejorada con campo para IP
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 240, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 30, 10, 30);

        JLabel title = new JLabel("Bienvenido al Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(0, 80, 160));
        panel.add(title, gbc);

        JLabel subtitle = new JLabel("Ingresa tus datos para comenzar");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0, 80, 160));
        panel.add(subtitle, gbc);

        // Campo para nombre de usuario
        JLabel nameLabel = new JLabel("Nombre de usuario:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(nameLabel, gbc);

        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBackground(Color.WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        panel.add(nameField, gbc);

        // Campo para dirección IP del servidor
        JLabel ipLabel = new JLabel("Dirección IP del servidor:");
        ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(ipLabel, gbc);

        JTextField ipField = new JTextField(20);
        ipField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ipField.setText("localhost"); // Valor por defecto
        ipField.setBackground(Color.WHITE);
        ipField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        panel.add(ipField, gbc);

        int result = JOptionPane.showConfirmDialog(null, panel, "Inicio de sesión",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String serverIP = ipField.getText().trim();
            if (!name.isEmpty() && !serverIP.isEmpty()) {
                SwingUtilities.invokeLater(() -> new ChatClientGUI(name, serverIP));
            }
        }
    }
}