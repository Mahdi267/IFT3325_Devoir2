public class Frame {
    private static final byte FLAG = 0b01111110;
    private byte type;
    private char num; // numéro encodé sur 3 bits
    private String data;
    private CRC crc;

    // Constructors
    public Frame(byte type, char num, String data, CRC crc) {
        this.type = type;
        this.num = num;
        this.data = data;
        this.crc = crc;
    }
    // Constructor pour une demande de connexion
    public Frame(byte type, String data, CRC crc) {
        this.type = type;
        this.data = data;
        this.crc = crc;

        // Dans le cas d'une demande de connexion, on met num = 0
        this.num = '0';
    }

    // Methods
    static String combineToBinaryString(byte type, char num, String data) {
        // Convertir 'type' en binaire sur 8 bits
        // Convertir 'num' en binaire sur 3 bits
        // Ajouter les données dans la chaîne binaire
        return String.format("%8s", Integer.toBinaryString(type & 0xFF))
                .replace(" ", "0") +
                String.format("%8s", Integer.toBinaryString(num & 0x07))
                        .replace(" ", "0") +
                data;
    }

    public String computeCRC(){
        String newData = combineToBinaryString(type, num, data);
        return crc.computeCRC(newData);
    }

    public void applyBitStuffing(){

    }

    public void removeBitStuffing(){

    }

    public void convertToByte(){

    }

    public String buildFrameFromBytes(){
        StringBuilder frame = new StringBuilder();

        String flag_insert = String.format("%8s", Integer.toBinaryString(FLAG & 0xFF)).
                replace(" ", "0");

        // Ajouter le flag au début
        frame.append(flag_insert);

        // Ajouter type, num et données
        frame.append(combineToBinaryString(type, num, data));

        // Ajouter CRC
        frame.append(computeCRC());

        // Ajouter le flag à la fin
        frame.append(flag_insert);

        return frame.toString();
    }

    @Override
    public String toString() {
        String flag_print = String.format("%8s", Integer.toBinaryString(FLAG & 0xFF)).
                replace(' ', '0');
        return "Frame {" +
                "Flag=" + flag_print +
                ", Type=" + (char) type +
                ", Num=" + num +
                ", Data='" + data + '\'' +
                ", CRC='" + computeCRC() + '\'' +
                ", Flag=" + flag_print +
                '}';
    }

    // Getter et Setter
    public byte getFlag() {return FLAG;}
    public byte getType() {return type;}
    public void setType(byte type) {this.type = type;}
    public char getNum() {return num;}
    public void setNum(char num) {this.num = num;}
    public String getData() {return data;}
    public void setData(String data) {this.data = data;}
    public CRC getCrc() {return crc;}
    public void setCrc(CRC crc) {this.crc = crc;}
}
