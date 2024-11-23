import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class Sender {
    // Attributes
    private String fileName;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private int numberPort;
    private String nomMachine;
    private int windowSize = 4;
    private List<Frame> framesList = new ArrayList<>();
    private int base = 0;
    private int nextFrameNumber = 0;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ConcurrentHashMap<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    // Constructor
    public Sender(String fileName, String host, int port) throws IOException {
        this.fileName = fileName;
        this.socket = new Socket(host, port);
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Methods
    public void readData() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = reader.readLine()) != null) {
            framesList.add(new Frame((byte) 'I', (byte)(framesList.size()%8), line, new CRC()));
        }
        reader.close();
    }

    public void sendFrame(Frame frame) {
        writer.println(frame.buildFrameFromBytes());
        // Démarrer un temporisateur pour cette trame
        int num = frame.getNum();
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            resendFrame(num);
        }, 3, TimeUnit.SECONDS);
        timers.put(num, timer);
    }

    public void resendFrame(int frameNumber){
        System.out.println("Timeout for frame: " + frameNumber + ", resending from base...");
        // Renvoyer toutes les trames de base à frameNumber->Next
        for (int i = base; i < nextFrameNumber; i++){
            Frame frame = framesList.get(i);
            sendFrame(frame);
        }
    }

    public void manageAck() throws Exception {
        String response;
        while ((response = reader.readLine()) != null) {
            Frame ack = Frame.unBuildFrame(response);
            if (ack.getType() == 'A'){
                int num = ack.getNum();
                System.out.println("Received ACK for: " + num);
                if (num >= base){
                    // Stopper les temporisateur des trames ackitter
                    for (int i = base; i < num; i++){
                        ScheduledFuture<?> timer = timers.get(i);
                        if (timer != null){
                            timer.cancel(true);
                            timers.remove(i);
                        }
                    }
                    base = num + 1;
                    // Continuer d'envoyer des trames tant que possible
                    while(nextFrameNumber < base + windowSize && nextFrameNumber < framesList.size()){
                        sendFrame(framesList.get(nextFrameNumber));
                        nextFrameNumber++;
                    }
                }
            } else if (ack.getType() == 'R'){
                int rejectNum = ack.getNum();
                System.out.println("Received REJ for: " + rejectNum);
                // Renvoyer toutes les trames à partir de rejectNum
                base = rejectNum;
                nextFrameNumber = rejectNum;
                for (int i = 0; i < nextFrameNumber; i++){
                    sendFrame(framesList.get(i));
                }
            }
            // Fin de communication
            if (ack.getType() == 'F'){
                System.out.println("Communication terminée!");
                break;
            }
        }
    }

    public void start() throws Exception {
        Frame frame = new Frame((byte)'C', (byte)0, "", new CRC());
        sendFrame(frame);
        nextFrameNumber++;

        // Gérer les acks dans un thread séparé
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                manageAck();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Envoyer les trames de données
        while (base < framesList.size()){
            while (nextFrameNumber < base + windowSize && nextFrameNumber < framesList.size()){
                sendFrame(framesList.get(nextFrameNumber));
                nextFrameNumber++;
            }
            // Attendre les acks
            Thread.sleep(100);
        }

        Frame finalFrame = new Frame((byte)'F', (byte)0, "", new CRC());
        sendFrame(finalFrame);
        executor.shutdown();
        scheduler.shutdown();
        socket.close();
    }

    // Communication entre Receiver et Sender
    public static void main(String[] args) {
        try {
            Sender sender = new Sender(args[0], args[1], Integer.parseInt(args[2]));
            sender.readData();
            sender.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
