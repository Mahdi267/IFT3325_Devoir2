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

    public Sender() {
        this.nextFrameToSend = 0;
        this.base = 0;
        this.window = new Frame[WINDOW_SIZE];
        this.isConnected = false;
        this.buffer = new byte[1024];
        this.timer = new Timer(TIMEOUT);
        this.timer.setTimeoutHandler(() -> {
            handleTimeout();
        });
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
        System.out.println("Timeout - Resending frames from " + base + " to " + (nextFrameToSend-1));
        for (int i = base; i < nextFrameToSend; i++) {
            if (window[i % WINDOW_SIZE] != null) {
                try {
                    String frameString = window[i % WINDOW_SIZE].buildFrameFromBytes();
                    out.write(frameString.getBytes());
                    out.flush();
                    System.out.println("Resent frame " + (window[i % WINDOW_SIZE].getNum() - '0'));
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
        }

        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = fileReader.readLine()) != null && isConnected) {
                String binaryData = stringToBinary(line);

                while (!canSendNextFrame()) {
                    waitForAck();
                }

                Frame frame = new Frame((byte)'I', (char)('0' + (nextFrameToSend % 8)), binaryData, new CRC());
                sendFrame(frame);
            }

            // Envoyer la trame de fin
            Frame endFrame = new Frame((byte)'F', (char)'0', "", new CRC());
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

    public void sendFrame(Frame frame) {
        try {
            frame.applyBitStuffing();
            String frameString = frame.buildFrameFromBytes();
            byte[] frameBytes = frameString.getBytes();
            out.write(frameBytes);
            out.flush();

            if (frame.getType() == 'I') {
                window[nextFrameToSend % WINDOW_SIZE] = frame;
                System.out.println("Sent I-frame " + (frame.getNum() - '0') +
                        " (Data length: " + frame.getData().length() + ")");

                if (base == nextFrameToSend) {
                    timer.start();
                }
                nextFrameToSend++;

                // Attendre l'ACK
                boolean ackReceived = waitForAck();
                if (!ackReceived) {
                    System.out.println("No ACK received, will retransmit");
                }
            } else {
                System.out.println("Sent control frame: Type=" + (char)frame.getType() +
                        ", Num=" + (frame.getNum() - '0'));

                if (frame.getType() == 'C') {
                    waitForConnectionAck();
                }
            }

            Thread.sleep(100);

        } catch (IOException e) {
            System.out.println("Error sending frame: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean waitForConnectionAck() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TIMEOUT) {
            Frame ackFrame = receiveFrame();
            if (ackFrame != null && ackFrame.getType() == 'A') {
                System.out.println("Connection acknowledged");
                isConnected = true;
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean waitForAck() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TIMEOUT) {
            Frame response = receiveFrame();
            if (response != null) {
                if (response.getType() == 'A') {
                    int ackNum = response.getNum() - '0';
                    System.out.println("Received ACK for frame " + ackNum);
                    base = (ackNum + 1) % 8;
                    if (base == nextFrameToSend) {
                        timer.stop();
                    } else {
                        timer.stop();
                        timer.start();
                    }
                    return true;
                } else if (response.getType() == 'R') {
                    handleRejection(response);
                    return true;
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void handleRejection(Frame rejFrame) {
        int rejNum = rejFrame.getNum() - '0';
        System.out.println("Received REJ for frame " + rejNum);
        timer.stop();
        nextFrameToSend = rejNum; // PROBLÈME ?

        // Retransmettre à partir de rejNum
        System.out.println("Retransmitting from frame " + rejNum);
        for (int i = rejNum; i < nextFrameToSend; i++) {
            Frame frame = window[i % WINDOW_SIZE];
            if (frame != null) {
                try {
                    frame.applyBitStuffing();
                    out.write(frame.buildFrameFromBytes().getBytes());
                    out.flush();
                    System.out.println("Retransmitted frame " + (frame.getNum() - '0'));
                } catch (IOException e) {
                    System.out.println("Error retransmitting frame: " + e.getMessage());
                }
            }
        }
        timer.start();
    }

    public Frame receiveFrame() {
        try {
            if (in.available() > 0) {
                int bytesRead = in.read(buffer);
                if (bytesRead > 0) {
                    String frameData = new String(buffer, 0, bytesRead);
                    return parseFrameData(frameData);
                }
            }
        } catch (IOException e) {
            System.out.println("Error receiving frame: " + e.getMessage());
        }
        return null;
    }

    private Frame parseFrameData(String frameData) {
        try {
            if (!frameData.startsWith("01111110") || !frameData.endsWith("01111110")) {
                return null;
            }

            String data = frameData.substring(8, frameData.length() - 8);
            String typeBits = data.substring(0, 8);
            byte type = (byte) Integer.parseInt(typeBits, 2);
            String numBits = data.substring(8, 11);
            char num = (char)('0' + Integer.parseInt(numBits, 2));
            String payload = data.substring(11, data.length() - 16);

            Frame frame = new Frame(type, num, payload, new CRC());
            frame.removeBitStuffing(); // ENLEVER LE BIT STUFFING AVANT ?
            return frame;

        } catch (Exception e) {
            System.out.println("Error parsing frame: " + e.getMessage());
            return null;
        }
    }

    private boolean canSendNextFrame() {
        return (nextFrameToSend - base) < WINDOW_SIZE;
    }

    public void close() {
        try {
            timer.stop();
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