public class Frame {
    private static final byte FLAG = 0b01111110;
    private byte type;
    private byte num; // numéro encodé sur 3 bits
    private String data;
    private CRC crc;

    // Constructors
    public Frame(byte type, byte num, String data, CRC crc) {
        this.type = type;
        this.num = num;
        this.data = data;
        this.crc = crc;
    }

    // Methods
    static String combineToBinaryString(byte type, byte num, String data) {
        // Convertir 'type' en binaire sur 8 bits
        // Convertir 'num' en binaire sur 3 bits
        // Ajouter les données dans la chaîne binaire
        return String.format("%8s", Integer.toBinaryString(type & 0xFF))
                .replace(" ", "0") +
                String.format("%8s", Integer.toBinaryString(num & 0xFF))
                        .replace(" ", "0") +
                data;
    }

    public String computeCRC(){
        String newData = combineToBinaryString(type, num, data);
        return crc.computeCRC(newData);
    }

    public String applyBitStuffing(){
        String combined = combineToBinaryString(type, num, data);
        String crcStr = computeCRC();
        String toStuff = combined + crcStr;
        return BitStuffing.applyBitStuffing(toStuff);
    }

    public void removeBitStuffing(String stuffedFrame) throws Exception {
        String unstuffed = BitStuffing.removeBitStuffing(stuffedFrame);

        // Vérifier que la longueur minimale est respectée (Type + Num + CRC)
        if (unstuffed.length() < 8 + 8 + 16) {
            throw new Exception("Trame trop courte après suppression du bit stuffing.");
        }

        // Extraire Type (8 bits)
        String typeStr = unstuffed.substring(0, 8);
        this.type = (byte) Integer.parseInt(typeStr, 2);

        // Extraire Num (8 bits, seuls les 3 bits de poids faible sont utilisés)
        String numStr = unstuffed.substring(8, 16);
        this.num = (byte) (Integer.parseInt(numStr, 2) & 0b00000111); // Garder seulement les 3 bits

        // Extraire Données et CRC
        String dataAndCrc = unstuffed.substring(16);

        // Extraire CRC (16 bits)
        String crcStr = dataAndCrc.substring(dataAndCrc.length() - 16);
        this.crc.setCrcBits(crcStr);

        // Extraire Données
        this.data = dataAndCrc.substring(0, dataAndCrc.length() - 16);
    }

    public String buildFrameFromBytes(){
        StringBuilder frame = new StringBuilder();

        String flag_insert = String.format("%8s", Integer.toBinaryString(FLAG & 0xFF)).
                replace(" ", "0");
        // Ajouter le flag au début
        frame.append(flag_insert);

        // Appliquer le bit stuffing et ajouter le contenu
        String stuffedContent = applyBitStuffing();
        frame.append(stuffedContent);

        // Ajouter le flag à la fin
        frame.append(flag_insert);

        return frame.toString();
    }

    public static Frame unBuildFrame(String bitStream) throws Exception {
        String flagStr = String.format("%8s", Integer.toBinaryString(FLAG & 0xFF)).replace(" ", "0");

        // Vérifier les flags de début et de fin
        if (!bitStream.startsWith(flagStr) || !bitStream.endsWith(flagStr)) {
            throw new Exception("Trame invalide : flags manquants ou incorrects.");
        }

        // Extraire le contenu entre les flags
        String stuffedContent = bitStream.substring(flagStr.length(), bitStream.length() - flagStr.length());

        // Créer une nouvelle trame et supprimer le bit stuffing
        Frame frame = new Frame((byte)0, (byte)0, "", new CRC()); // Initialisation temporaire
        frame.removeBitStuffing(stuffedContent);

        // Vérifier le CRC
        String computedCRC = frame.computeCRC();
        if (!computedCRC.equals(frame.crc.getCrcBits())) { // Suppose que CRC a une méthode getCrcBits()
            throw new Exception("Erreur de CRC : trame corrompue.");
        }

        return frame;
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
    public byte getNum() {return num;}
    public void setNum(byte num) {this.num = num;}
    public String getData() {return data;}
    public void setData(String data) {this.data = data;}
    public CRC getCrc() {return crc;}
    public void setCrc(CRC crc) {this.crc = crc;}
}
