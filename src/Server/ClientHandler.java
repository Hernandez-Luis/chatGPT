package Server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String userName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error al crear flujos para el cliente.");
        }
    }

    @Override
    public void run() {
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

    private void broadcastMessage(String message) {
        for (ClientHandler client : ChatServer.clients) {
            if (client != this) { // No enviar el mensaje al mismo cliente
                client.output.println(message);
            }
        }
    }
}