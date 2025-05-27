package Server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String userName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PrintWriter getOutput() {
        return output;
    }

    public DataOutputStream getDataOut() {
        return dataOut;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("IMAGE:")) {
                    String fileName = line.substring(6);
                    int length = dataIn.readInt();
                    byte[] imageData = new byte[length];
                    dataIn.readFully(imageData);

                    // Enviar a todos los clientes
                    for (ClientHandler client : ChatServer.clients) {
                        client.getOutput().println("IMAGE:" + fileName);
                        client.getDataOut().writeInt(length);
                        client.getDataOut().write(imageData);
                        client.getDataOut().flush();
                    }
                } else {
                    try {
                        // Primero recibimos el nombre de usuario
                        String joinMessage = input.readLine();
                        if (joinMessage.startsWith("JOIN:")) {
                            this.userName = joinMessage.substring(5);
                            broadcastMessage(userName + " se ha unido al chat");
                        }

                        String message;
                        while ((message = input.readLine()) != null) {
                            System.out.println("[" + userName + "] " + message);
                            broadcastMessage("[" + userName + "] " + message);
                        }
                    } catch (IOException e) {
                        System.out.println(userName + " se ha desconectado.");
                    } finally {
                        try {
                            socket.close();
                            ChatServer.clients.remove(this);
                            broadcastMessage(userName + " ha abandonado el chat");
                        } catch (IOException e) {
                            System.out.println("Error al cerrar socket.");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado.");
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : ChatServer.clients) {
            if (client != this) { // No enviar el mensaje al mismo cliente
                client.output.println(message);
            }
        }
    }
}
