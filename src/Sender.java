import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Sender {
    public void readData(){}
    public void buildFrame(){}
    public void sendFrame(){}
    public void waitAck(){}
    public void manageAck(){}
    public void resendData(){}

    // Communication entre Receiver et Sender
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Connecting to Receiver...");
            Socket socket = new Socket("localhost", 12345);
            System.out.println("Connected to Receiver.");

            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            System.out.println("Enter messages to send (type 'exit' to quit):");
            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
                writer.println(message);
            }

            System.out.println("Closing connection...");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
