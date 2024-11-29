import java.util.ArrayList;
import java.util.List;
import java.io.*;

/**
 * Classe représentant une trame utilisée pour le transfert de données binaires.
 * La trame inclut des informations telles que le type, un numéro, les données,
 * et un CRC (Cyclic Redundancy Check) pour la vérification d'intégrité.
 *
 * <p>Cette classe gère la construction et l'analyse des trames en appliquant
 * des mécanismes de bit stuffing et de byte stuffing pour assurer une transmission
 * fiable et éviter les ambiguïtés liées aux flags de délimitation.</p>
 *
 * <p>Types de trames supportés :</p>
 * <ul>
 *     <li><b>'C'</b> : Trame de connexion</li>
 *     <li><b>'I'</b> : Trame d'information (données)</li>
 *     <li><b>'F'</b> : Trame de fin de transmission</li>
 *     <li><b>'A'</b> : Trame d'accusé de réception (ACK)</li>
 *     <li><b>'R'</b> : Trame de rejet (REJ)</li>
 * </ul>
 */
public class Frame {
    /**
     * Flag de délimitation des trames. Utilisé pour marquer le début et la fin d'une trame.
     * Valeur en hexadécimal : 0x7E (01111110 en binaire).
     */
    public static final byte FLAG = 0x7E; // 01111110 en binaire

    /**
     * Octet d'échappement utilisé pour le byte stuffing.
     * Valeur en hexadécimal : 0x7D.
     */
    public static final byte ESCAPE = 0x7D;

    /**
     * Type de la trame.
     * Utilisé pour déterminer l'action à entreprendre lors de la réception de la trame.
     */
    private byte type;

    /**
     * Numéro de la trame, encodé sur 3 bits.
     * Permet de suivre l'ordre des trames et de gérer les accusés de réception.
     */
    private byte num; // numéro encodé sur 3 bits

    /**
     * Données contenues dans la trame.
     * Pour les trames de type 'I', cela représente les données transmises.
     * Pour d'autres types de trames, cela peut contenir des messages de contrôle ou être vide.
     */
    private String data;

    /**
     * Instance de la classe {@link CRC} utilisée pour calculer et vérifier le CRC de la trame.
     * Assure l'intégrité des données transmises.
     */
    private CRC crc;

    /**
     * Constructeur complet pour une trame.
     * Initialise tous les champs nécessaires à la création d'une trame valide.
     *
     * @param type Le type de la trame (par exemple, 'C', 'I', 'F').
     * @param num  Le numéro de la trame, encodé sur 3 bits.
     * @param data Les données contenues dans la trame.
     * @param crc  L'instance de {@link CRC} utilisée pour le calcul et la vérification du CRC.
     */
    public Frame(byte type, byte num, String data, CRC crc) {
        this.type = type;
        this.num = (byte) (num & 0x07); // Assurez-vous que num est sur 3 bits
        this.data = data;
        this.crc = crc;
    }

    /**
     * Constructeur simplifié pour une trame sans numéro explicite.
     * Initialise le numéro de trame à 0 par défaut.
     *
     * @param type Le type de la trame (par exemple, 'C', 'I', 'F').
     * @param data Les données contenues dans la trame.
     * @param crc  L'instance de {@link CRC} utilisée pour le calcul et la vérification du CRC.
     */
    public Frame(byte type, String data, CRC crc) {
        this.type = type;
        this.data = data;
        this.crc = crc;
        this.num = 0;
    }

    /**
     * Construit une trame sous forme de tableau de bytes.
     * Applique le bit stuffing et le byte stuffing, puis ajoute les flags de début et de fin.
     *
     * @return Un tableau de bytes représentant la trame complète avec bit stuffing, byte stuffing, et des FLAGs.
     */
    public byte[] buildFrame() {
        // Construire la donnée binaire combinée (Type, Num, Data)
        String combined = String.format("%8s", Integer.toBinaryString(type & 0xFF)).replace(' ', '0') +
                String.format("%8s", Integer.toBinaryString(num & 0x07)).replace(' ', '0') +
                stringToBinary(data);

        // Calculer le CRC
        String crcStr = CRC.computeCRC(combined);
        this.crc.setCrcBits(crcStr); // Mettre à jour crcBits
        combined += crcStr;

        // Appliquer le bit stuffing
        String stuffed = BitStuffing.applyBitStuffing(combined);

        // Convertir en bytes
        byte[] stuffedBytes = BitStuffing.binaryStringToBytes(stuffed);

        // Appliquer le byte stuffing
        byte[] stuffedAndEscapedBytes = applyByteStuffing(stuffedBytes);

        // Construire la trame finale avec les FLAGs
        List<Byte> frame = new ArrayList<>();
        frame.add(FLAG); // FLAG de début
        for (byte b : stuffedAndEscapedBytes) {
            frame.add(b);
        }
        frame.add(FLAG); // FLAG de fin

        // Convertir en tableau de bytes
        byte[] frameArray = new byte[frame.size()];
        for (int i = 0; i < frame.size(); i++) {
            frameArray[i] = frame.get(i);
        }

        return frameArray;
    }

    /**
     * Analyse une trame reçue sous forme de tableau de bytes.
     * Décode les informations de la trame, enlève le byte stuffing et le bit stuffing,
     * puis vérifie l'intégrité du CRC.
     *
     * @param frameData Un tableau de bytes représentant la trame reçue.
     * @return Une instance de {@link Frame} représentant la trame décodée.
     * @throws Exception Si la trame est invalide, trop courte, ou si le CRC ne correspond pas.
     */
    public static Frame parseFrame(byte[] frameData) throws Exception {
        if (frameData.length < 4) {
            throw new Exception("Trame trop courte.");
        }

        if (frameData[0] != FLAG || frameData[frameData.length - 1] != FLAG) {
            throw new Exception("Flags de début ou de fin incorrects.");
        }

        // Extraire le contenu entre les FLAGs
        byte[] content = new byte[frameData.length - 2];
        System.arraycopy(frameData, 1, content, 0, frameData.length - 2);

        // Supprimer le byte stuffing
        byte[] unescapedContent = removeByteStuffing(content);

        // Convertir les bytes en chaîne binaire
        String bitString = BitStuffing.bytesToBinaryString(unescapedContent);

        // Supprimer le bit stuffing
        String unstuffed = BitStuffing.removeBitStuffing(bitString);

        // Vérifier la longueur minimale (Type + Num + CRC)
        if (unstuffed.length() < 8 + 8 + 16) {
            throw new Exception("Trame trop courte après suppression du bit stuffing.");
        }

        // Extraire le type
        String typeStr = unstuffed.substring(0, 8);
        byte type = (byte) Integer.parseInt(typeStr, 2);

        // Extraire le numéro
        String numStr = unstuffed.substring(8, 16);
        byte num = (byte) (Integer.parseInt(numStr, 2) & 0x07); // Garder seulement les 3 bits

        // Extraire le CRC
        String crcStr = unstuffed.substring(unstuffed.length() - 16);
        CRC crc = new CRC();
        crc.setCrcBits(crcStr); // Conserver le crcBits extrait

        // Extraire les données
        String dataBits = unstuffed.substring(16, unstuffed.length() - 16);

        // Combiner Type, Num et Data pour le calcul du CRC
        String combinedData = typeStr + numStr + dataBits;

        // Calculer le CRC pour vérifier l'intégrité
        String computedCRC = CRC.computeCRC(combinedData);

        // Vérifier si le CRC correspond
        if (!computedCRC.equals(crcStr)) {
            throw new Exception("Erreur de CRC : trame corrompue.");
        }

        // Convertir les données binaires en chaîne de caractères
        String data = binaryToString(dataBits);

        return new Frame(type, num, data, crc);
    }

    /**
     * Calcule le CRC pour une chaîne de données binaires donnée.
     *
     * @param data La chaîne binaire sur laquelle calculer le CRC.
     * @return Une chaîne binaire représentant les bits du CRC.
     */
    public String computeCRC(String data) {
        return CRC.computeCRC(data);
    }

    /**
     * Convertit une chaîne binaire en une chaîne de caractères.
     *
     * @param binary La chaîne binaire à convertir.
     * @return Une chaîne de caractères représentant les données binaires.
     */
    private static String binaryToString(String binary) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            String byteStr = binary.substring(i, Math.min(i + 8, binary.length()));
            result.append((char) Integer.parseInt(byteStr, 2));
        }
        return result.toString();
    }

    /**
     * Convertit une chaîne de caractères en une représentation binaire.
     *
     * @param input La chaîne de caractères à convertir.
     * @return Une chaîne binaire représentant les caractères.
     */
    private String stringToBinary(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            result.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return result.toString();
    }

    /**
     * Applique le byte stuffing pour échapper les octets spéciaux (FLAG et ESCAPE).
     *
     * @param data Le tableau de bytes à traiter.
     * @return Un tableau de bytes avec le byte stuffing appliqué.
     */
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

    /**
     * Supprime le byte stuffing d'un tableau de bytes.
     *
     * @param data Le tableau de bytes à traiter.
     * @return Un tableau de bytes sans byte stuffing.
     * @throws Exception Si une séquence d'échappement est invalide.
     */
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
            throw new Exception("Séquence d'échappement invalide en fin de données.");
        }
        return outputStream.toByteArray();
    }

    /**
     * Retourne une représentation textuelle de la trame.
     *
     * @return Une chaîne représentant les détails de la trame.
     */
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

    /**
     * Obtient le type de la trame.
     *
     * @return Le type de la trame sous forme de byte.
     */
    public byte getType() {
        return type;
    }

    /**
     * Définit le type de la trame.
     *
     * @param type Le type de la trame sous forme de byte.
     */
    public void setType(byte type) {
        this.type = type;
    }

    /**
     * Obtient le numéro de la trame.
     *
     * @return Le numéro de la trame encodé sur 3 bits.
     */
    public byte getNum() {
        return num;
    }

    /**
     * Définit le numéro de la trame.
     *
     * @param num Le numéro de la trame sous forme de byte.
     *            Seules les 3 bits de poids faible sont conservés.
     */
    public void setNum(byte num) {
        this.num = (byte) (num & 0x07);
    }

    /**
     * Obtient les données de la trame.
     *
     * @return Les données de la trame sous forme de chaîne de caractères.
     */
    public String getData() {
        return data;
    }

    /**
     * Définit les données de la trame.
     *
     * @param data Les données de la trame sous forme de chaîne de caractères.
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Obtient l'instance de {@link CRC} associée à la trame.
     *
     * @return L'instance de {@link CRC}.
     */
    public CRC getCrc() {
        return crc;
    }

    /**
     * Définit l'instance de {@link CRC} associée à la trame.
     *
     * @param crc L'instance de {@link CRC} à associer à la trame.
     */
    public void setCrc(CRC crc) {
        this.crc = crc;
    }
}
