import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Timer;
import java.io.InputStream;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) {
        byte type = 'I';
        char num = '5';
        String data = "11010101";
        CRC crc = new CRC();

        Frame frame = new Frame(type, num, data, crc);
        System.out.println(frame);

        
    }
}
