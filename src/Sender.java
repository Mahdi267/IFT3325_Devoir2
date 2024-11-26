import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Sender {
    private static final int WINDOW_SIZE = 8;
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
        System.out.println("Timeout - Resending frames from " + base + " to " + (nextFrameToSend - 1));
        for (int i = base; i < nextFrameToSend; i++) {
            if (window[i % WINDOW_SIZE] != null) {
                try {
                    String frameString = window[i % WINDOW_SIZE].buildFrameFromBytes();
                    out.write(frameString.getBytes());
                    out.flush();
                    System.out.println("Resent frame " + (window[i % WINDOW_SIZE].getNum() & 0b00000111));
                } catch (IOException e) {
                    System.out.println("Error resending frame: " + e.getMessage());
                }
            }
        }
        timer.start();
    }

    public void connect() {
        try {
            System.out.println("Initiating connection with Go-Back-N...");
            Frame connFrame = new Frame((byte)'C', "GoBackN", new CRC());
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
            startAckListener(); // Démarrer le listener des ACK après la connexion
        }

        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = fileReader.readLine()) != null && isConnected) {
                String binaryData = stringToBinary(line);

                // Envoyer autant de trames que possible jusqu'à remplir la fenêtre
                while (canSendNextFrame() && (line != null)) {
                    byte num = (byte) (nextFrameToSend % WINDOW_SIZE);
                    Frame frame = new Frame((byte) 'I', num, binaryData, new CRC());
                    sendFrame(frame);
                    line = fileReader.readLine();
                    if (line != null) {
                        binaryData = stringToBinary(line);
                    }
                }
            }

            // Envoyer la trame de fin
            Frame endFrame = new Frame((byte) 'F', (byte) 0, "", new CRC());
            sendFrame(endFrame);

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private String stringToBinary(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            result.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return result.toString();
    }

    public synchronized void sendFrame(Frame frame) {
        try {
            String frameString = frame.buildFrameFromBytes();
            byte[] frameBytes = frameString.getBytes();
            out.write(frameBytes);
            out.flush();

            if (frame.getType() == 'I') {
                window[nextFrameToSend % WINDOW_SIZE] = frame;
                System.out.println("Sent I-frame " + (frame.getNum() & 0b00000111) +
                        " (Data length: " + frame.getData().length() + ")");

                if (base == nextFrameToSend) {
                    timer.start();
                }
                nextFrameToSend++;

                // Ne pas attendre l'ACK ici ; continuer à envoyer des trames dans la fenêtre
            } else {
                System.out.println("Sent control frame: Type=" + (char)frame.getType() +
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
        if ((base <= ackNum && ackNum < nextFrameToSend) ||
                (base > nextFrameToSend && (ackNum < nextFrameToSend || ackNum >= base))) {
            base = (ackNum + 1) % WINDOW_SIZE;
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
        nextFrameToSend = rejNum;

        // Retransmettre les trames à partir de rejNum
        System.out.println("Retransmitting from frame " + rejNum);
        for (int i = base; i < base + WINDOW_SIZE && i < nextFrameToSend; i++) {
            Frame frame = window[i % WINDOW_SIZE];
            if (frame != null) {
                try {
                    String frameString = frame.buildFrameFromBytes();
                    out.write(frameString.getBytes());
                    out.flush();
                    System.out.println("Retransmitted frame " + (frame.getNum() & 0b00000111));
                } catch (IOException e) {
                    System.out.println("Error retransmitting frame: " + e.getMessage());
                }
            }
        }
        timer.start();
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
            if (in.available() > 0) {
                int bytesRead = in.read(buffer);
                if (bytesRead > 0) {
                    String frameData = new String(buffer, 0, bytesRead);
                    return Frame.unBuildFrame(frameData);
                }
            }
        } catch (IOException e) {
            System.out.println("Error receiving frame: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error parsing frame: " + e.getMessage());
        }
        return null;
    }

    private boolean canSendNextFrame() {
        return (nextFrameToSend - base) < WINDOW_SIZE;
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
