public class BitStuffing {
    /**
     * Applique le bit stuffing sur une chaîne binaire.
     * Ajoute un '0' après chaque séquence de cinq '1' consécutifs.
     * @param binaryData La chaîne binaire à encoder
     * @return La chaîne binaire avec bit stuffing appliqué
     */
    public static String applyBitStuffing(String binaryData) {
        if (binaryData == null || binaryData.isEmpty()) {
            return binaryData;
        }

        StringBuilder stuffedData = new StringBuilder();
        int consecutiveOnes = 0;

        for (int i = 0; i < binaryData.length(); i++) {
            char currentBit = binaryData.charAt(i);
            stuffedData.append(currentBit);

            if (currentBit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    stuffedData.append('0');
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;
            }
        }

        return stuffedData.toString();
    }

    /**
     * Retire le bit stuffing d'une chaîne binaire.
     * Retire les '0' qui ont été ajoutés après chaque séquence de cinq '1' consécutifs.
     * @param stuffedData La chaîne binaire à décoder
     * @return La chaîne binaire originale sans bit stuffing
     */
    public static String removeBitStuffing(String stuffedData) {
        if (stuffedData == null || stuffedData.isEmpty()) {
            return stuffedData;
        }

        StringBuilder unstuffedData = new StringBuilder();
        int consecutiveOnes = 0;

        for (int i = 0; i < stuffedData.length(); i++) {
            char currentBit = stuffedData.charAt(i);
            unstuffedData.append(currentBit);

            if (currentBit == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    // Vérifiez si le prochain bit est un '0' et l'ignorez
                    if (i + 1 < stuffedData.length() && stuffedData.charAt(i + 1) == '0') {
                        i++; // Sauter le '0' de stuffing
                    }
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;
            }
        }

        return unstuffedData.toString();
    }

    /**
     * Convertit une chaîne binaire en tableau de bytes.
     * @param binaryString La chaîne binaire à convertir
     * @return Le tableau de bytes correspondant
     */
    public static byte[] binaryStringToBytes(String binaryString) {
        if (!isBinaryString(binaryString)) {
            throw new IllegalArgumentException("La chaîne doit être binaire (contenir uniquement des '0' et des '1').");
        }

        // Ajouter du padding si nécessaire pour avoir un multiple de 8 bits
        int padding = 8 - (binaryString.length() % 8);
        if (padding != 8) {
            binaryString = binaryString + "0".repeat(padding);
        }

        byte[] bytes = new byte[binaryString.length() / 8];
        for (int i = 0; i < bytes.length; i++) {
            String byteString = binaryString.substring(i * 8, (i + 1) * 8);
            bytes[i] = (byte) Integer.parseInt(byteString, 2);
        }

        return bytes;
    }

    /**
     * Convertit un tableau de bytes en chaîne binaire.
     * @param bytes Le tableau de bytes à convertir
     * @return La chaîne binaire correspondante
     */
    public static String bytesToBinaryString(byte[] bytes) {
        StringBuilder binaryString = new StringBuilder();
        for (byte b : bytes) {
            binaryString.append(String.format("%8s",
                    Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return binaryString.toString();
    }

    /**
     * Vérifie si une chaîne contient uniquement des '0' et des '1'.
     * @param data La chaîne à vérifier
     * @return true si la chaîne est binaire, false sinon
     */
    private static boolean isBinaryString(String data) {
        return data.matches("[01]+");
    }
}