import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Receiver {
    public void receiveFrame(){}
    public void checkError(){}
    public void sendAck(){}
    public void sendRej(){}

    // Communication entre Receiver et Sender
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Receiver is running and waiting for connections...");

            Socket socket = serverSocket.accept();
            System.out.println("Sender connected.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Received: " + message);
            }

            System.out.println("Sender disconnected.");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
