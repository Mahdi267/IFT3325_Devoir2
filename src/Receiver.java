import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Receiver {
    // Attributes
    private final ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private int nextNumberExpected; // Prochain numéro de trame

    // Constructor
    public Receiver(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    // Methods
    public void start() throws IOException {
        System.out.println("Receveur attend une connexion...");
        socket = serverSocket.accept();
        System.out.println("Envoyer connecté");

        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String received;
        while ((received = reader.readLine()) != null) {
            try {
                Frame frame = Frame.unBuildFrame(received);
                if (frame.getType() == 'C') {
                    // Répondre à la demander de connexion
                    Frame ack = new Frame((byte)'A', (byte)0, "", new CRC());
                    writer.println(ack);
                    System.out.println("La connexion a été établie avec succès");
                } else if (frame.getType() == 'I') {
                    // Vérifier le numéro de la trame et le CRC
                    if (frame.getNum() == nextNumberExpected) {
                        System.out.println("Reception de la trame attendue: " + frame.getNum());
                        nextNumberExpected = (nextNumberExpected + 1) % 8;
                        // Envoyer un ack
                        Frame ack = new Frame((byte)'A', frame.getNum(), "", new CRC());
                        writer.println(ack.buildFrameFromBytes());
                    } else {
                        System.out.println("Trame innatendue: " + frame.getNum() + ", attendait: " + nextNumberExpected);
                        // Envoyer un rej
                        Frame rej = new Frame((byte)'R', (byte)nextNumberExpected, "", new CRC());
                        writer.println(rej.buildFrameFromBytes());
                    }
                } else if (frame.getType() == 'F') {
                    System.out.println("Trame de fermeture reçue: " + frame.getNum());
                    Frame ack = new Frame((byte)'A', (byte)0, "", new CRC());
                    writer.println(ack.buildFrameFromBytes());
                    break;
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de la gestions des trames" + e.getMessage());
                // Envoyer un rej
                Frame rej = new Frame((byte)'R', (byte)nextNumberExpected, "", new CRC());
                writer.println(rej.buildFrameFromBytes());
            }
        }
        System.out.println("Envoyeur déconnecté");
        socket.close();
        serverSocket.close();
    }

    // Communication entre Receiver et Sender
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            Receiver receiver = new Receiver(12345);
            receiver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
