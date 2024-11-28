import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Timer;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Classe principale pour la transmission de données utilisant le protocole Go-Back-N.
 * Ce programme peut fonctionner en mode émetteur (sender) ou récepteur (receiver) selon les arguments fournis.
 *
 * <p>Modes d'utilisation :</p>
 * <ul>
 *     <li>Émetteur (Sender) : <code>java Main sender &lt;hostname&gt; &lt;port&gt; &lt;filename&gt; &lt;0&gt;</code></li>
 *     <li>Récepteur (Receiver) : <code>java Main receiver &lt;port&gt;</code></li>
 * </ul>
 *
 * <p>Exemples :</p>
 * <ul>
 *     <li>Émettre un fichier : <code>java Main sender localhost 8080 data.txt 0</code></li>
 *     <li>Recevoir des données : <code>java Main receiver 8080</code></li>
 * </ul>
 */
public class Main {

    /**
     * Démarre le mode émetteur.
     * Initialise le Sender, lit les données du fichier spécifié et les envoie au récepteur.
     *
     * @param hostName Le nom de l'hôte ou l'adresse IP du récepteur.
     * @param port     Le port de destination pour la connexion.
     * @param filename Le chemin du fichier à envoyer.
     */
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

    /**
     * Démarre le mode récepteur.
     * Initialise le Receiver, accepte une connexion entrante et traite les trames reçues jusqu'à la fin de la transmission.
     *
     * @param port Le port sur lequel le récepteur doit écouter les connexions entrantes.
     */
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

    /**
     * Point d'entrée principal du programme.
     * Permet de lancer le programme en mode émetteur ou récepteur selon les arguments fournis.
     *
     * @param args Les arguments de la ligne de commande.
     *             <ul>
     *                 <li>Pour le mode émetteur : <code>sender &lt;hostname&gt; &lt;port&gt; &lt;filename&gt; &lt;0&gt;</code></li>
     *                 <li>Pour le mode récepteur : <code>receiver &lt;port&gt;</code></li>
     *             </ul>
     *
     *             <p>Exemples :</p>
     *             <ul>
     *                 <li><code>java Main sender localhost 8080 data.txt 0</code></li>
     *                 <li><code>java Main receiver 8080</code></li>
     *             </ul>
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        try {
            if (args[0].equalsIgnoreCase("sender")) {
                if (args.length != 5) {
                    System.out.println("Incorrect number of arguments for sender.");
                    printUsage();
                    return;
                }
                String hostname = args[1];
                int port = Integer.parseInt(args[2]);
                String filename = args[3];
                // Vérifier que le dernier argument est "0" pour Go-Back-N
                if (!args[4].equals("0")) {
                    System.out.println("Last argument must be 0 for Go-Back-N");
                    printUsage();
                    return;
                }
                runSender(hostname, port, filename);
            } else if (args[0].equalsIgnoreCase("receiver")) {
                if (args.length != 2) {
                    System.out.println("Incorrect number of arguments for receiver.");
                    printUsage();
                    return;
                }
                int port = Integer.parseInt(args[1]);
                runReceiver(port);
            } else {
                System.out.println("Invalid mode. Use 'sender' or 'receiver'");
                printUsage();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche les instructions d'utilisation du programme.
     * Fournit des exemples de commandes pour les modes émetteur et récepteur.
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  Sender:   java Main sender <hostname> <port> <filename> <0>");
        System.out.println("  Receiver: java Main receiver <port>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java Main sender localhost 8080 data.txt 0");
        System.out.println("  java Main receiver 8080");
    }
}
