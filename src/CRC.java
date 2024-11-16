public class CRC {
    // CRC-CCITT (x^16 + x^12 + x^5 +1), pour faire la division
    private static final String crc_ccitt = "10001000000100001";
    private static final int crc_ccitt_length = crc_ccitt.length();

    // Constructor
    public CRC(){}

    // Permet d'effectuer l'oppération Xor
    static String Xor (String a, String b) {
        StringBuilder result = new StringBuilder();
        int n = a.length();

        for (int i = 0; i < n; i++) {
            result.append(a.charAt(i) == b.charAt(i) ? '0' : '1');
        }
        return result.toString();
    }

    // Permet de calculer la division Mod 2 de deux nombres binaires
    static String Mod2Div(String dividend) {
        int n = dividend.length();
        int divisorLength = crc_ccitt.length(); // 17 bits
        String divisor = crc_ccitt;
        String tmp = dividend.substring(0, divisorLength);

        while (divisorLength < n) {
            if (tmp.charAt(0) == '1') {
                tmp = Xor(divisor, tmp);
            } else {
                tmp = Xor("0".repeat(divisor.length()), tmp);
            }

            // Ajouter le prochain bit du dividende
            tmp = tmp.substring(1) + dividend.charAt(divisorLength);
            divisorLength++;
        }

        // Dernière itération pour obtenir le reste final
        if (tmp.charAt(0) == '1') {
            tmp = Xor(divisor, tmp);
        } else {
            tmp = Xor("0".repeat(divisor.length()), tmp);
        }

        // Le reste final est de 16 bits
        return tmp.substring(1);
    }

    public String computeCRC(String data){
        int zerosToAdd = crc_ccitt_length - 1;

        // Ajouter les zéros nécessaires à la fin de la chaîne
        String binaryData = data + "0".repeat(zerosToAdd);

        return String.format("%16s", Mod2Div(binaryData)).replace(' ', '0');
    }
}
