import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Timer;
import java.io.InputStream;
import java.io.OutputStream;

public class Main {
    public static void runSender(String hostName, int port, String filename) {
        try {
            System.out.println("Starting Sender...");
            Sender sender = new Sender();
            sender.initialize(hostName, port, filename);

            // Établir la connexion et envoyer les données
            try {
                sender.readData();
                System.out.println("Transmission complete.");
            } catch (Exception e) {
                System.out.println("Error during transmission: " + e.getMessage());
            } finally {
                sender.close();
            }
        } catch (Exception e) {
            System.out.println("Failed to start sender: " + e.getMessage());
        }
    }

    public static void runReceiver(int port) {
        try {
            System.out.println("Starting Receiver...");
            Receiver receiver = new Receiver();
            receiver.initialize(port);

            try {
                receiver.acceptConnection();
                System.out.println("Connection accepted, waiting for frames...");

                while (true) {
                    Frame frame = receiver.receiveFrame();
                    if (frame == null) continue;

                    receiver.processFrame(frame);

                    // Si c'est une trame de fin, terminer
                    if (frame.getType() == 'F') {
                        System.out.println("End of transmission received.");
                        break;
                    }
                }
            } finally {
                receiver.close();
            }
        } catch (Exception e) {
            System.out.println("Failed to start receiver: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage:");
            System.out.println("Sender: java Main sender <hostname> <port> <filename> <0>");
            System.out.println("Receiver: java Main receiver <port>");
            return;
        }

        try {
            if (args[0].equals("sender")) {
                if (args.length != 5) {
                    System.out.println("Usage: java Main sender <hostname> <port> <filename> <0>");
                    return;
                }
                String hostname = args[1];
                int port = Integer.parseInt(args[2]);
                String filename = args[3];
                // Vérifier que le dernier argument est "0" pour Go-Back-N
                if (!args[4].equals("0")) {
                    System.out.println("Last argument must be 0 for Go-Back-N");
                    return;
                }
                runSender(hostname, port, filename);
            } else if (args[0].equals("receiver")) {
                if (args.length != 2) {
                    System.out.println("Usage: java Main receiver <port>");
                    return;
                }
                int port = Integer.parseInt(args[1]);
                runReceiver(port);
            } else {
                System.out.println("Invalid mode. Use 'sender' or 'receiver'");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}