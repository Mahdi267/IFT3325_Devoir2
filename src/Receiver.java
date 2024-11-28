import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Classe représentant un récepteur pour la transmission de données utilisant le protocole Go-Back-N.
 * Le récepteur écoute sur un port donné, traite les trames reçues et envoie des accusés de réception (ACK) ou des rejets (REJ).
 */
public class Receiver {
    /**
     * Socket serveur pour écouter les connexions entrantes.
     */
    private ServerSocket serverSocket;

    /**
     * Numéro de trame attendu actuellement.
     */
    private int expectedFrameNumber;

    /**
     * Indique si une connexion est établie avec un émetteur.
     */
    private boolean isConnected;

    /**
     * Socket client pour communiquer avec l'émetteur connecté.
     */
    private Socket clientSocket;

    /**
     * Flux de sortie vers l'émetteur pour envoyer des ACK ou REJ.
     */
    private OutputStream out;

    /**
     * Flux d'entrée depuis l'émetteur pour recevoir des trames.
     */
    private InputStream in;

    /**
     * Tampon utilisé pour la lecture des données entrantes.
     */
    private byte[] buffer;

    /**
     * Indique si le récepteur est en cours d'exécution.
     */
    private volatile boolean running;

    /**
     * Constructeur par défaut du Receiver.
     * Initialise les variables nécessaires.
     */
    public Receiver() {
        this.expectedFrameNumber = 0;
        this.isConnected = false;
        this.buffer = new byte[1024];
        this.running = true;
    }

    /**
     * Initialise le récepteur en écoutant sur un port donné.
     *
     * @param port Le port sur lequel le récepteur doit écouter.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de l'initialisation.
     */
    public void initialize(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("Receiver initialized and waiting on port " + port);
    }

    /**
     * Accepte une connexion entrante d'un émetteur.
     *
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de l'acceptation de la connexion.
     */
    public void acceptConnection() throws IOException {
        System.out.println("Waiting for connection...");
        clientSocket = serverSocket.accept();
        out = clientSocket.getOutputStream();
        in = clientSocket.getInputStream();
        isConnected = true;
        System.out.println("Connected to sender: " + clientSocket.getInetAddress());
    }

    /**
     * Reçoit une trame depuis l'entrée du socket.
     *
     * @return Une instance de la classe {@link Frame} représentant la trame reçue, ou {@code null} en cas d'erreur.
     */
    public Frame receiveFrame() {
        try {
            ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
            int b;
            boolean startFlagFound = false;

            // Lire jusqu'à trouver le FLAG de début
            while ((b = in.read()) != -1) {
                if ((byte) b == Frame.FLAG) {
                    startFlagFound = true;
                    break;
                }
            }

            if (!startFlagFound) {
                return null;
            }

            // Lire jusqu'au FLAG de fin
            while ((b = in.read()) != -1) {
                if ((byte) b == Frame.FLAG) {
                    break;
                }
                frameBuffer.write(b);
            }

            byte[] frameContent = frameBuffer.toByteArray();
            byte[] fullFrame = new byte[frameContent.length + 2];
            fullFrame[0] = Frame.FLAG;
            System.arraycopy(frameContent, 0, fullFrame, 1, frameContent.length);
            fullFrame[fullFrame.length - 1] = Frame.FLAG;

            return Frame.parseFrame(fullFrame);

        } catch (IOException e) {
            System.out.println("Error receiving frame: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error parsing frame: " + e.getMessage());
        }
        return null;
    }

    /**
     * Traite une trame reçue et envoie un ACK ou un REJ en fonction du numéro de trame.
     *
     * @param frame La trame à traiter.
     */
    public void processFrame(Frame frame) {
        if (frame == null) {
            System.out.println("Received invalid frame, sending REJ");
            sendRej(expectedFrameNumber);
            return;
        }

        try {
            switch ((char) frame.getType()) {
                case 'C':
                    System.out.println("Received connection request");
                    sendAck(0);
                    System.out.println("Connection established");
                    break;

                case 'I':
                    int frameNum = frame.getNum() & 0b00000111;
                    if (frameNum == expectedFrameNumber) {
                        String receivedData = frame.getData();
                        System.out.println("Received frame " + expectedFrameNumber + ": " + receivedData);
                        sendAck(expectedFrameNumber);
                        expectedFrameNumber = (expectedFrameNumber + 1) % 8;
                    } else {
                        System.out.println("Out of sequence. Expected " + expectedFrameNumber + ", got " + frameNum);
                        sendRej(expectedFrameNumber);
                    }
                    break;

                case 'F':
                    System.out.println("End of transmission received");
                    int finalFrameNum = frame.getNum() & 0b00000111;
                    if (finalFrameNum == expectedFrameNumber) {
                        sendAck(finalFrameNum);
                        expectedFrameNumber = (expectedFrameNumber + 1) % 8;
                        System.out.println("Closing connection...");
                        close();
                    } else {
                        System.out.println("Out of sequence for F frame. Expected " + expectedFrameNumber + ", got " + finalFrameNum);
                        sendRej(expectedFrameNumber);
                    }
                    break;

                default:
                    System.out.println("Unknown frame type: " + (char) frame.getType());
            }
        } catch (Exception e) {
            System.out.println("Error processing frame: " + e.getMessage());
        }
    }

    /**
     * Envoie un accusé de réception (ACK) pour une trame donnée.
     *
     * @param frameNum Le numéro de trame à accuser réception.
     */
    public void sendAck(int frameNum) {
        try {
            if (!isConnected) return;

            Frame ackFrame = new Frame((byte) 'A', (byte) frameNum, "", new CRC());
            byte[] ackBytes = ackFrame.buildFrame();
            out.write(ackBytes);
            out.flush();
            System.out.println("Sent ACK for frame " + frameNum + "\n");
        } catch (IOException e) {
            System.out.println("\nError sending ACK: " + e.getMessage() + "\n");
        }
    }

    /**
     * Envoie un rejet (REJ) pour une trame donnée.
     *
     * @param frameNum Le numéro de trame à rejeter.
     */
    public void sendRej(int frameNum) {
        try {
            if (!isConnected) return;

            Frame rejFrame = new Frame((byte) 'R', (byte) frameNum, "", new CRC());
            byte[] rejBytes = rejFrame.buildFrame();
            out.write(rejBytes);
            out.flush();
            System.out.println("Sent REJ for frame " + frameNum);
        } catch (IOException e) {
            System.out.println("Error sending REJ: " + e.getMessage());
        }
    }

    /**
     * Ferme toutes les ressources associées au récepteur.
     * Cela inclut les flux, les sockets et le serveur.
     */
    public synchronized void close() {
        try {
            running = false;
            isConnected = false;

            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();

            System.out.println("Receiver closed");
        } catch (IOException e) {
            System.out.println("Error closing receiver: " + e.getMessage());
        }
    }

    /**
     * Vérifie si le récepteur est en cours d'exécution.
     *
     * @return {@code true} si le récepteur est actif, sinon {@code false}.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Point d'entrée principal pour le récepteur.
     * Initialise et démarre le récepteur sur un port spécifié.
     *
     * @param args Arguments de la ligne de commande. Doit contenir un seul argument : le port.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Receiver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Receiver receiver = new Receiver();

        try {
            receiver.initialize(port);
            receiver.acceptConnection();

            while (receiver.running) {
                Frame frame = receiver.receiveFrame();
                if (frame != null) {
                    receiver.processFrame(frame);
                } else {
                    System.out.println("No frame received or invalid frame.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            receiver.close();
        }
    }

    /**
     * Méthode utilitaire pour ajouter les flags de début et de fin à une trame de contenu.
     *
     * @param content Le contenu de la trame sans flags.
     * @return Un tableau de bytes représentant la trame complète avec flags.
     */
    private byte[] concatenateFlags(byte[] content) {
        byte[] framed = new byte[content.length + 2];
        framed[0] = Frame.FLAG;
        System.arraycopy(content, 0, framed, 1, content.length);
        framed[framed.length - 1] = Frame.FLAG;
        return framed;
    }
}
