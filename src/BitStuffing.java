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
                if (consecutiveOnes == 5 && i + 1 < stuffedData.length() 
                    && stuffedData.charAt(i + 1) == '0') {
                    i++;  // Sauter le '0' de stuffing
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;
            }
        }
        
        return unstuffedData.toString();
    }

    private static boolean isBinaryString(String data) {
        return data.matches("[01]+");
    }


    public static byte[] binaryStringToBytes(String binaryString) {
        if (!isBinaryString(binaryString)) {
            throw new IllegalArgumentException("La chaîne doit être binaire (contenir uniquement des '0' et des '1')");
        }

        // Ajouter du padding si nécessaire pour avoir un multiple de 8 bits
        int padding = 8 - (binaryString.length() % 8);
        if (padding != 8) {
            StringBuilder paddedString = new StringBuilder(binaryString);
            for (int i = 0; i < padding; i++) {
                paddedString.append('0');
            }
            binaryString = paddedString.toString();
        }

        byte[] bytes = new byte[binaryString.length() / 8];
        for (int i = 0; i < bytes.length; i++) {
            String byteString = binaryString.substring(i * 8, (i + 1) * 8);
            bytes[i] = (byte) Integer.parseInt(byteString, 2);
        }

        return bytes;
    }


    public static String bytesToBinaryString(byte[] bytes) {
        StringBuilder binaryString = new StringBuilder();
        for (byte b : bytes) {
            binaryString.append(String.format("%8s", 
                Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return binaryString.toString();
    }
}