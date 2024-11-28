import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Receiver {
    private ServerSocket serverSocket;
    private int expectedFrameNumber;
    private boolean isConnected;
    private Socket clientSocket;
    private OutputStream out;
    private InputStream in;
    private byte[] buffer;
    private volatile boolean running;

    public Receiver() {
        this.expectedFrameNumber = 0;
        this.isConnected = false;
        this.buffer = new byte[1024];
        this.running = true;
    }

    public void initialize(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("Receiver initialized and waiting on port " + port);
    }

    public void acceptConnection() throws IOException {
        System.out.println("Waiting for connection...");
        clientSocket = serverSocket.accept();
        out = clientSocket.getOutputStream();
        in = clientSocket.getInputStream();
        isConnected = true;
        System.out.println("Connected to sender: " + clientSocket.getInetAddress());
    }

    public Frame receiveFrame() {
        try {
            ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
            int b;
            boolean startFlagFound = false;

            // Read until start FLAG
            while ((b = in.read()) != -1) {
                if ((byte) b == Frame.FLAG) {
                    startFlagFound = true;
                    break;
                }
            }

            if (!startFlagFound) {
                return null;
            }

            // Read until end FLAG
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

    // Méthode utilitaire pour ajouter les flags de début et de fin
    private byte[] concatenateFlags(byte[] content) {
        byte[] framed = new byte[content.length + 2];
        framed[0] = Frame.FLAG;
        System.arraycopy(content, 0, framed, 1, content.length);
        framed[framed.length - 1] = Frame.FLAG;
        return framed;
    }

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

    // Convertit une chaîne binaire en une chaîne de caractères
    private String binaryToString(String binary) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            String byteStr = binary.substring(i, Math.min(i + 8, binary.length()));
            result.append((char) Integer.parseInt(byteStr, 2));
        }
        return result.toString();
    }

    public boolean isRunning() {
        return running;
    }

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
}
