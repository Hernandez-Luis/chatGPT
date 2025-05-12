package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    public static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(9090)) {
            System.out.println("Servidor iniciado. Esperando clientes...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
