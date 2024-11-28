import java.io.ByteArrayOutputStream;

/**
 * Classe utilitaire pour gérer le bit stuffing et le bit unstuffing dans les transmissions de trames.
 * Le bit stuffing est utilisé pour éviter la confusion entre les données de trame et les flags de délimitation.
 *
 * <p>Cette classe fournit des méthodes pour appliquer le bit stuffing, retirer le bit stuffing, et convertir entre
 * chaînes binaires et tableaux de bytes.</p>
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *     <li>Appliquer le bit stuffing sur une chaîne binaire.</li>
 *     <li>Retirer le bit stuffing d'une chaîne binaire.</li>
 *     <li>Convertir une chaîne binaire en tableau de bytes.</li>
 *     <li>Convertir un tableau de bytes en chaîne binaire.</li>
 * </ul>
 */
public class BitStuffing {

    /**
     * Applique le bit stuffing sur une chaîne binaire.
     * Ajoute un '0' après chaque séquence de cinq '1' consécutifs pour éviter les séquences conflictuelles avec le flag.
     *
     * @param binaryData La chaîne binaire à encoder.
     * @return La chaîne binaire avec bit stuffing appliqué.
     * @throws IllegalArgumentException Si la chaîne d'entrée est nulle.
     */
    public static String applyBitStuffing(String binaryData) {
        if (binaryData == null) {
            throw new IllegalArgumentException("La chaîne binaire ne peut pas être nulle.");
        }
        if (binaryData.isEmpty()) {
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
                    stuffedData.append('0'); // Ajouter un '0' après cinq '1'
                    consecutiveOnes = 0; // Réinitialiser le compteur
                }
            } else {
                consecutiveOnes = 0; // Réinitialiser le compteur si le bit n'est pas '1'
            }
        }

        return stuffedData.toString();
    }

    /**
     * Retire le bit stuffing d'une chaîne binaire.
     * Supprime les '0' qui ont été ajoutés après chaque séquence de cinq '1' consécutifs.
     *
     * @param stuffedData La chaîne binaire à décoder.
     * @return La chaîne binaire originale sans bit stuffing.
     * @throws IllegalArgumentException Si la chaîne d'entrée est nulle.
     */
    public static String removeBitStuffing(String stuffedData) {
        if (stuffedData == null) {
            throw new IllegalArgumentException("La chaîne binaire ne peut pas être nulle.");
        }
        if (stuffedData.isEmpty()) {
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
                    // Vérifier si le prochain bit est un '0' de stuffing
                    if (i + 1 < stuffedData.length() && stuffedData.charAt(i + 1) == '0') {
                        i++; // Ignorer le '0' de stuffing
                    }
                    consecutiveOnes = 0; // Réinitialiser le compteur
                }
            } else {
                consecutiveOnes = 0; // Réinitialiser le compteur si le bit n'est pas '1'
            }
        }

        return unstuffedData.toString();
    }

    /**
     * Convertit une chaîne binaire en un tableau de bytes.
     * Ajoute du padding avec des '0' si nécessaire pour que la longueur soit un multiple de 8 bits.
     *
     * @param binaryString La chaîne binaire à convertir.
     * @return Le tableau de bytes correspondant.
     * @throws IllegalArgumentException Si la chaîne d'entrée n'est pas binaire.
     */
    public static byte[] binaryStringToBytes(String binaryString) {
        if (binaryString == null) {
            throw new IllegalArgumentException("La chaîne binaire ne peut pas être nulle.");
        }

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
     * Convertit un tableau de bytes en une chaîne binaire.
     *
     * @param bytes Le tableau de bytes à convertir.
     * @return La chaîne binaire correspondante.
     */
    public static String bytesToBinaryString(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Le tableau de bytes ne peut pas être nul.");
        }

        StringBuilder binaryString = new StringBuilder();
        for (byte b : bytes) {
            binaryString.append(String.format("%8s",
                    Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return binaryString.toString();
    }

    /**
     * Vérifie si une chaîne contient uniquement des '0' et des '1'.
     *
     * @param data La chaîne à vérifier.
     * @return {@code true} si la chaîne est binaire, {@code false} sinon.
     */
    private static boolean isBinaryString(String data) {
        return data.matches("[01]+");
    }
}
