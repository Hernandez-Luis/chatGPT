package Client;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 9090)) {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            // Hilo que recibe mensajes del servidor
            Thread reader = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = input.readLine()) != null) {
                        System.out.println(">> " + msg);
                    }
                } catch (IOException e) {
                    System.out.println("Desconectado del servidor.");
                }
            });

            reader.start();

            // Envío de mensajes desde la consola
            System.out.println("Puedes comenzar a chatear. Escribe tu mensaje:");
            String message;
            while ((message = console.readLine()) != null) {
                output.println(message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
