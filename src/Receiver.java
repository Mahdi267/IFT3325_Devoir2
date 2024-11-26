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
        if (!isConnected || !running) return null;

        try {
            int bytesRead = in.read(buffer);
            if (bytesRead > 0) {
                String frameData = new String(buffer, 0, bytesRead);
                System.out.println("Received raw frame: " + frameData);
                Frame frame = Frame.unBuildFrame(frameData);
                if (frame != null) {
                    return frame;
                }
            }
        } catch (SocketException se) {
            if (running) {
                System.out.println("Connection lost: " + se.getMessage());
                running = false;
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("Error receiving frame: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error parsing frame: " + e.getMessage());
        }
        return null;
    }

    public void processFrame(Frame frame) {
        if (frame == null) {
            System.out.println("Received invalid frame, sending REJ");
            sendRej(expectedFrameNumber);
            return;
        }

        try {
            switch ((char)frame.getType()) {
                case 'C':
                    System.out.println("Received connection request");
                    sendAck(0);
                    System.out.println("Connection established");
                    break;

                case 'I':
                    int frameNum = frame.getNum() & 0b00000111;
                    if (frameNum == expectedFrameNumber) {
                        String receivedData = binaryToString(frame.getData());
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
                    sendAck(expectedFrameNumber);
                    System.out.println("Closing connection...");
                    close();
                    break;

                default:
                    System.out.println("Unknown frame type: " + (char)frame.getType());
            }
        } catch (Exception e) {
            System.out.println("Error processing frame: " + e.getMessage());
        }
    }

    public void sendAck(int frameNum) {
        try {
            if (!isConnected) return;

            Frame ackFrame = new Frame((byte)'A', (byte)frameNum, "", new CRC());
            String frameString = ackFrame.buildFrameFromBytes();
            out.write(frameString.getBytes());
            out.flush();
            System.out.println("Sent ACK for frame " + frameNum);
        } catch (IOException e) {
            System.out.println("Error sending ACK: " + e.getMessage());
        }
    }

    public void sendRej(int frameNum) {
        try {
            if (!isConnected) return;

            Frame rejFrame = new Frame((byte)'R', (byte)frameNum, "", new CRC());
            String frameString = rejFrame.buildFrameFromBytes();
            out.write(frameString.getBytes());
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

    private String binaryToString(String binary) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            String byte_str = binary.substring(i, Math.min(i + 8, binary.length()));
            result.append((char)Integer.parseInt(byte_str, 2));
        }
        return result.toString();
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
