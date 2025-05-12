package Server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

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
            String message;
            while ((message = input.readLine()) != null) {
                System.out.println("Mensaje recibido: " + message);
                broadcastMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar socket.");
            }
        }
    }

    // Envía mensaje a todos los clientes
    private void broadcastMessage(String message) {
        for (ClientHandler client : ChatServer.clients) {
            client.output.println(message);
        }
    }
}
