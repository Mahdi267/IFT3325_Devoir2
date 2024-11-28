import java.io.*;
import java.net.Socket;

public class Sender {
    private static final int WINDOW_SIZE = 4; // Réduit de 8 à 4
    private static final int TIMEOUT = 3000;
    private Socket socket;
    private int nextFrameToSend;
    private int base;
    private Frame[] window;
    private Timer timer;
    private String filename;
    private boolean isConnected;
    private OutputStream out;
    private InputStream in;
    private byte[] buffer;
    private Thread ackListenerThread;
    private boolean fSent = false;
    private boolean fAcked = false;
    private final Object ackLock = new Object();

    public Sender() {
        this.nextFrameToSend = 0;
        this.base = 0;
        this.window = new Frame[WINDOW_SIZE];
        this.isConnected = false;
        this.buffer = new byte[1024];
        this.timer = new Timer(TIMEOUT);
        this.timer.setTimeoutHandler(this::handleTimeout);
    }

    public void initialize(String hostName, int port, String filename) {
        this.filename = filename;
        try {
            this.socket = new Socket(hostName, port);
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
            System.out.println("Sender initialized - Connected to " + hostName + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTimeout() {
        System.out.println("Timeout - Resending frames from " + base + " to " + ((nextFrameToSend - 1 + 8) % 8));
        int i = base;
        while (i != nextFrameToSend) {
            Frame frame = window[i % WINDOW_SIZE];
            if (frame != null) {
                try {
                    byte[] frameBytes = frame.buildFrame();
                    out.write(frameBytes);
                    out.flush();
                    System.out.println("Resent frame " + (frame.getNum() & 0b00000111));
                } catch (IOException e) {
                    System.out.println("Error resending frame: " + e.getMessage());
                }
            }
            i = (i + 1) % 8;
        }
        timer.start();
    }

    public void connect() {
        try {
            System.out.println("Initiating connection with Go-Back-N...");
            CRC crc = new CRC();
            Frame connFrame = new Frame((byte) 'C', (byte) 0, "Go-Back-N", crc);
            sendFrame(connFrame);

            long startTime = System.currentTimeMillis();
            while (!isConnected && (System.currentTimeMillis() - startTime) < TIMEOUT) {
                Frame respFrame = receiveFrame();
                if (respFrame != null && respFrame.getType() == 'A') {
                    isConnected = true;
                    System.out.println("Connection established with Go-Back-N");
                    break;
                }
                Thread.sleep(100);
            }

            if (!isConnected) {
                throw new IOException("Connection timeout");
            }
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void readData() {
        if (!isConnected) {
            connect();
            if (!isConnected) {
                System.out.println("Unable to establish connection. Exiting.");
                return;
            }
            startAckListener(); // Start the ACK listener after the connection is established
        }

        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            boolean endOfFileReached = false;
            String nextLine = null;

            // Lire la première ligne du fichier
            nextLine = fileReader.readLine();
            if (nextLine == null) {
                // Le fichier est vide, envoyer directement la trame de fin
                Frame endFrame = new Frame((byte) 'F', (byte) (nextFrameToSend & 0x07), "", new CRC());
                sendFrame(endFrame);
                fSent = true;

                // Attendre l'ACK de la trame 'F'
                synchronized (ackLock) {
                    while (!fAcked) {
                        ackLock.wait();
                    }
                }
                return;
            }

            while (true) {
                // Envoyer des trames si la fenêtre n'est pas pleine et que le fichier n'est pas terminé
                while (canSendNextFrame() && !endOfFileReached) {
                    byte num = (byte) (nextFrameToSend & 0x07);
                    CRC crc = new CRC();
                    Frame frame = new Frame((byte) 'I', num, nextLine, crc);
                    sendFrame(frame);

                    // Lire la prochaine ligne pour la prochaine itération
                    nextLine = fileReader.readLine();
                    if (nextLine == null) {
                        endOfFileReached = true;
                    }
                }

                // Vérifier si le fichier est terminé et toutes les trames sont acquittées
                if (endOfFileReached && base == nextFrameToSend) {
                    // Envoyer la trame de fin
                    Frame endFrame = new Frame((byte) 'F', (byte) (nextFrameToSend & 0x07), "", new CRC());
                    sendFrame(endFrame);
                    fSent = true;

                    // Attendre l'ACK de la trame 'F'
                    synchronized (ackLock) {
                        while (!fAcked) {
                            ackLock.wait();
                        }
                    }
                    break;
                }

                // Attendre un petit moment avant de vérifier à nouveau
                Thread.sleep(10);
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            close();
        }
    }

    public synchronized void sendFrame(Frame frame) {
        try {
            byte[] frameBytes = frame.buildFrame();
            out.write(frameBytes);
            out.flush();

            if (frame.getType() == 'I' || frame.getType() == 'F') {
                window[nextFrameToSend % WINDOW_SIZE] = frame;
                System.out.println("Sent frame " + (frame.getNum() & 0b00000111) +
                        " (Type: " + (char) frame.getType() + ", Data length: " + frame.getData().length() + ")");

                if (base == nextFrameToSend) {
                    timer.start();
                }
                nextFrameToSend = (nextFrameToSend + 1) % 8;
            } else {
                System.out.println("Sent control frame: Type=" + (char) frame.getType() +
                        ", Num=" + (frame.getNum() & 0b00000111));
                if (frame.getType() == 'C') {
                    waitForConnectionAck();
                }
            }

        } catch (IOException e) {
            System.out.println("Error sending frame: " + e.getMessage());
        }
    }

    private void startAckListener() {
        ackListenerThread = new Thread(() -> {
            while (isConnected) {
                Frame response = receiveFrame();
                if (response != null) {
                    if (response.getType() == 'A') {
                        handleAck(response);
                    } else if (response.getType() == 'R') {
                        handleRejection(response);
                    }
                }
                try {
                    Thread.sleep(10); // Petite pause
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        ackListenerThread.start();
    }

    private synchronized void handleAck(Frame ackFrame) {
        int ackNum = ackFrame.getNum() & 0b00000111;
        System.out.println("Received ACK for frame " + ackNum);

        // Vérifier si l'ACK est pour la trame 'F'
        if (fSent && ackNum == ((nextFrameToSend - 1 + 8) % 8)) {
            fAcked = true;
            synchronized (ackLock) {
                ackLock.notifyAll();
            }
        }

        // Gestion des ACK pour les trames de données
        if (isSeqNumBetween(base, (nextFrameToSend - 1 + 8) % 8, ackNum)) {
            base = (ackNum + 1) % 8;
            if (base == nextFrameToSend) {
                timer.stop();
            } else {
                timer.stop();
                timer.start();
            }
        }
    }

    private synchronized void handleRejection(Frame rejFrame) {
        int rejNum = rejFrame.getNum() & 0b00000111;
        System.out.println("Received REJ for frame " + rejNum);
        timer.stop();
        base = rejNum;

        // Retransmettre les trames à partir de rejNum jusqu'à nextFrameToSend - 1
        System.out.println("Retransmitting from frame " + rejNum);
        int i = base;
        while (i != nextFrameToSend) {
            Frame frame = window[i % WINDOW_SIZE];
            if (frame != null) {
                try {
                    byte[] frameBytes = frame.buildFrame();
                    out.write(frameBytes);
                    out.flush();
                    System.out.println("Retransmitted frame " + (frame.getNum() & 0b00000111));
                } catch (IOException e) {
                    System.out.println("Error retransmitting frame: " + e.getMessage());
                }
            }
            i = (i + 1) % 8;
        }
        timer.start(); // Redémarrer le timer après la retransmission
    }

    private void waitForConnectionAck() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TIMEOUT) {
            try {
                Frame ackFrame = receiveFrame();
                if (ackFrame != null && ackFrame.getType() == 'A') {
                    System.out.println("Connection acknowledged");
                    isConnected = true;
                    return;
                }
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public Frame receiveFrame() {
        try {
            // Lire jusqu'au flag de début
            ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
            int b;
            boolean startFlagFound = false;

            while ((b = in.read()) != -1) {
                if (b == Frame.FLAG) {
                    startFlagFound = true;
                    break;
                }
            }

            if (!startFlagFound) {
                return null;
            }

            // Lire jusqu'au flag de fin
            while ((b = in.read()) != -1) {
                if (b == Frame.FLAG) {
                    break;
                }
                frameBuffer.write(b);
            }

            if (b != Frame.FLAG) {
                throw new Exception("Trame invalide : flag de fin manquant.");
            }

            byte[] frameContent = frameBuffer.toByteArray();
            return Frame.parseFrame(concatenateFlags(frameContent));

        } catch (IOException e) {
            System.out.println("Error receiving frame: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error parsing frame: " + e.getMessage());
        }
        return null;
    }

    // Méthode utilitaire pour ajouter les flags de début et de fin
    private byte[] concatenateFlags(byte[] content) {
        byte[] framed = new byte[content.length + 2];
        framed[0] = Frame.FLAG;
        System.arraycopy(content, 0, framed, 1, content.length);
        framed[framed.length - 1] = Frame.FLAG;
        return framed;
    }

    private boolean canSendNextFrame() {
        return ((nextFrameToSend - base + 8) % 8) < WINDOW_SIZE;
    }

    public synchronized void close() {
        try {
            isConnected = false;
            timer.stop();
            if (ackListenerThread != null) {
                ackListenerThread.interrupt();
                ackListenerThread = null;
            }
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("Sender closed");
        } catch (IOException e) {
            System.out.println("Error closing sender: " + e.getMessage());
        }
    }

    private boolean isSeqNumBetween(int start, int end, int num) {
        start = (start + 8) % 8;
        end = (end + 8) % 8;
        num = (num + 8) % 8;

        if (start <= end) {
            return num >= start && num <= end;
        } else {
            // Cas où la séquence a bouclé
            return num >= start || num <= end;
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java Sender <hostname> <port> <filename> <0>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String filename = args[2];

        Sender sender = new Sender();
        sender.initialize(hostname, port, filename);
        try {
            sender.readData();
        } finally {
            sender.close();
        }
    }
}
