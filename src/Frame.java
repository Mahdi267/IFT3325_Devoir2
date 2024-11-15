public class Frame {
    private byte[] flag = {0b01111110};
    private char type;
    private byte[] num; // numéro encodé sur 3 bits
    private byte[] data;
    private CRC crc;
    private static final int crc_ccitt = 0b10001000000100001;

    // Constructor
    public Frame(char type, byte[] num, byte[] data, CRC crc) {
        this.type = type;
        this.num = num;
        this.data = data;
        this.crc = crc;
    }

    // Methods
    public int computeCRC(){
        return 0;
    }

    public void applyBitStuffing(){

    }

    public void removeBitStuffing(){

    }

    public void convertToByte(){

    }

    public void buildFrameFromBytes(){
    }

    // Getter et Setter
    public byte[] getFlag() {return flag;}
    public void setFlag(byte[] flag) {this.flag = flag;}
    public char getType() {return type;}
    public void setType(char type) {this.type = type;}
    public byte[] getNum() {return num;}
    public void setNum(byte[] num) {this.num = num;}
    public byte[] getData() {return data;}
    public void setData(byte[] data) {this.data = data;}
    public CRC getCrc() {return crc;}
    public void setCrc(CRC crc) {this.crc = crc;}
}
