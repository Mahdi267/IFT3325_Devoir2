import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class Frame {
    public static final byte FLAG = 0x7E; // 01111110 en binaire
    public static final byte ESCAPE = 0x7D;
    private byte type;
    private byte num; // numéro encodé sur 3 bits
    private String data;
    private CRC crc;

    // Constructors
    public Frame(byte type, byte num, String data, CRC crc) {
        this.type = type;
        this.num = (byte) (num & 0x07); // Assurez-vous que num est sur 3 bits
        this.data = data;
        this.crc = crc;
    }

    public Frame(byte type, String data, CRC crc) {
        this.type = type;
        this.data = data;
        this.crc = crc;
        this.num = 0;
    }

    // Construire la trame sous forme de tableau de bytes
    public byte[] buildFrame() {
        // Build combined binary data (Type, Num, Data)
        String combined = String.format("%8s", Integer.toBinaryString(type & 0xFF)).replace(' ', '0') +
                String.format("%8s", Integer.toBinaryString(num & 0x07)).replace(' ', '0') +
                stringToBinary(data);

        // Compute CRC
        String crcStr = computeCRC(combined);
        combined += crcStr;

        // Apply bit stuffing
        String stuffed = BitStuffing.applyBitStuffing(combined);

        // Convert to bytes
        byte[] stuffedBytes = BitStuffing.binaryStringToBytes(stuffed);

        // Apply byte stuffing
        byte[] stuffedAndEscapedBytes = applyByteStuffing(stuffedBytes);

        // Build frame with FLAGs
        List<Byte> frame = new ArrayList<>();
        frame.add(FLAG); // Start flag
        for (byte b : stuffedAndEscapedBytes) {
            frame.add(b);
        }
        frame.add(FLAG); // End flag

        // Convert to byte array
        byte[] frameArray = new byte[frame.size()];
        for (int i = 0; i < frame.size(); i++) {
            frameArray[i] = frame.get(i);
        }

        return frameArray;
    }

    // Parse une trame reçue sous forme de tableau de bytes
    public static Frame parseFrame(byte[] frameData) throws Exception {
        if (frameData.length < 4) {
            throw new Exception("Trame trop courte.");
        }

        if (frameData[0] != FLAG || frameData[frameData.length - 1] != FLAG) {
            throw new Exception("Flags de début ou de fin incorrects.");
        }

        // Extract content between FLAGs
        byte[] content = new byte[frameData.length - 2];
        System.arraycopy(frameData, 1, content, 0, frameData.length - 2);

        // Remove byte stuffing
        byte[] unescapedContent = removeByteStuffing(content);

        // Convert bytes to binary string
        String bitString = BitStuffing.bytesToBinaryString(unescapedContent);

        // Remove bit stuffing
        String unstuffed = BitStuffing.removeBitStuffing(bitString);

        // Vérifier la longueur minimale (Type + Num + CRC)
        if (unstuffed.length() < 8 + 8 + 16) {
            throw new Exception("Trame trop courte après suppression du bit stuffing.");
        }

        // Extraire Type
        String typeStr = unstuffed.substring(0, 8);
        byte type = (byte) Integer.parseInt(typeStr, 2);

        // Extraire Num
        String numStr = unstuffed.substring(8, 16);
        byte num = (byte) (Integer.parseInt(numStr, 2) & 0x07); // Garder seulement les 3 bits

        // Extraire CRC
        String crcStr = unstuffed.substring(unstuffed.length() - 16);
        CRC crc = new CRC();
        crc.setCrcBits(crcStr);

        // Extraire Data
        String dataBits = unstuffed.substring(16, unstuffed.length() - 16);

        // Combiner Type, Num et Data pour le calcul du CRC
        String combinedData = typeStr + numStr + dataBits;

        // Calculer le CRC
        String computedCRC = crc.computeCRC(combinedData);
        if (!computedCRC.equals(crc.getCrcBits())) {
            throw new Exception("Erreur de CRC : trame corrompue.");
        }

        // Convertir Data de binaire à chaîne
        String data = binaryToString(dataBits);

        return new Frame(type, num, data, crc);
    }

    // Calculer le CRC pour une chaîne binaire
    public String computeCRC(String data) {
        return crc.computeCRC(data);
    }

    // Convertir une chaîne binaire en chaîne de caractères
    private static String binaryToString(String binary) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            String byteStr = binary.substring(i, Math.min(i + 8, binary.length()));
            result.append((char) Integer.parseInt(byteStr, 2));
        }
        return result.toString();
    }

    private String stringToBinary(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            result.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return result.toString();
    }

    private byte[] applyByteStuffing(byte[] data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte b : data) {
            if (b == FLAG || b == ESCAPE) {
                outputStream.write(ESCAPE);
                outputStream.write((byte) (b ^ 0x20));
            } else {
                outputStream.write(b);
            }
        }
        return outputStream.toByteArray();
    }

    private static byte[] removeByteStuffing(byte[] data) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean escapeFlag = false;
        for (byte b : data) {
            if (escapeFlag) {
                outputStream.write((byte) (b ^ 0x20));
                escapeFlag = false;
            } else if (b == ESCAPE) {
                escapeFlag = true;
            } else {
                outputStream.write(b);
            }
        }
        if (escapeFlag) {
            throw new Exception("Invalid escape sequence at the end of data");
        }
        return outputStream.toByteArray();
    }

    @Override
    public String toString() {
        return "Frame {" +
                "Type=" + (char) type +
                ", Num=" + num +
                ", Data='" + data + '\'' +
                ", CRC='" + crc.getCrcBits() + '\'' +
                '}';
    }

    // Getters et Setters
    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }
    public byte getNum() { return num; }
    public void setNum(byte num) { this.num = (byte) (num & 0x07); }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public CRC getCrc() { return crc; }
    public void setCrc(CRC crc) { this.crc = crc; }
}
