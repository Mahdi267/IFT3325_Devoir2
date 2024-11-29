/**
 * Classe pour le calcul du CRC (Cyclic Redundancy Check) en utilisant le polynôme CRC-CCITT.
 */
public class CRC {
    /**
     * Polynôme CRC-CCITT (x^16 + x^12 + x^5 + 1) utilisé pour la division Mod 2.
     * Représenté sous forme de chaîne binaire.
     */
    private static final String CRC_CCITT = "10001000000100001"; // 17 bits

    /**
     * Longueur du polynôme CRC-CCITT.
     */
    private static final int CRC_CCITT_LENGTH = CRC_CCITT.length();

    /**
     * Les bits du CRC calculé, représentés sous forme de chaîne binaire.
     */
    private String crcBits;

    /**
     * Constructeur par défaut de la classe CRC.
     */
    public CRC() {}

    /**
     * Obtient les bits du CRC calculé.
     *
     * @return Les bits du CRC sous forme de chaîne binaire.
     */
    public String getCrcBits() {
        return crcBits;
    }

    /**
     * Définit les bits du CRC calculé.
     * Valide que la chaîne fournie est de longueur 16 et ne contient que des caractères '0' ou '1'.
     *
     * @param crcBits Les bits du CRC sous forme de chaîne binaire.
     * @throws IllegalArgumentException Si la chaîne fournie n'est pas valide.
     */
    public void setCrcBits(String crcBits) {
        if (crcBits.length() != 16 || !crcBits.matches("[01]+")) {
            throw new IllegalArgumentException("CRC invalide.");
        }
        this.crcBits = crcBits;
    }

    /**
     * Effectue une opération XOR (exclusive OR) entre deux chaînes binaires de même longueur.
     *
     * @param a La première chaîne binaire.
     * @param b La deuxième chaîne binaire.
     * @return Le résultat de l'opération XOR sous forme de chaîne binaire.
     */
    static String xor(String a, String b) {
        StringBuilder result = new StringBuilder();
        int n = a.length();

        for (int i = 0; i < n; i++) {
            result.append(a.charAt(i) == b.charAt(i) ? '0' : '1');
        }
        return result.toString();
    }

    /**
     * Effectue la division Mod 2 (bitwise) d'un dividende binaire par le polynôme CRC-CCITT.
     *
     * @param dividend La chaîne binaire représentant le dividende.
     * @return Le reste de la division Mod 2 sous forme de chaîne binaire.
     */
    static String mod2Div(String dividend) {
        int n = dividend.length();
        int divisorLength = CRC_CCITT_LENGTH; // 17 bits
        String divisor = CRC_CCITT;
        String tmp = dividend.substring(0, divisorLength);

        while (divisorLength < n) {
            if (tmp.charAt(0) == '1') {
                tmp = xor(divisor, tmp);
            } else {
                tmp = xor("0".repeat(divisor.length()), tmp);
            }

            // Ajouter le prochain bit du dividende
            tmp = tmp.substring(1) + dividend.charAt(divisorLength);
            divisorLength++;
        }

        // Dernière itération pour obtenir le reste final
        if (tmp.charAt(0) == '1') {
            tmp = xor(divisor, tmp);
        } else {
            tmp = xor("0".repeat(divisor.length()), tmp);
        }

        // Le reste final est de 16 bits
        return tmp.substring(1);
    }

    /**
     * Calcule le CRC pour une chaîne de données binaires donnée en utilisant le polynôme CRC-CCITT.
     *
     * @param data La chaîne binaire sur laquelle calculer le CRC.
     * @return Une chaîne binaire représentant les bits du CRC calculé.
     */
    public static String computeCRC(String data) {
        int zerosToAdd = CRC_CCITT_LENGTH - 1;

        // Ajouter les zéros nécessaires à la fin de la chaîne
        String binaryData = data + "0".repeat(zerosToAdd);

        // Effectuer la division Mod 2
        String computedCRC = String.format("%16s", mod2Div(binaryData)).replace(' ', '0');

        return computedCRC;
    }
}
