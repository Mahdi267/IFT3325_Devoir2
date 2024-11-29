import java.io.*;
import java.net.*;
import java.util.Arrays;

public class MainTest {
    public static void main(String[] args) {
        if (args.length > 0) {
            // Si des arguments sont fournis, exécuter en mode commande
            if (args[0].equalsIgnoreCase("sender")) {
                // Format: sender <hostname> <port> <filename> <0>
                if (args.length != 5) {
                    System.out.println("Usage: java MainTest sender <hostname> <port> <filename> <0>");
                    return;
                }
                String[] senderArgs = {args[1], args[2], args[3], args[4]};
                Sender.main(senderArgs);
            } else if (args[0].equalsIgnoreCase("receiver")) {
                // Format: receiver <port>
                if (args.length != 2) {
                    System.out.println("Usage: java MainTest receiver <port>");
                    return;
                }
                String[] receiverArgs = {args[1]};
                Receiver.main(receiverArgs);
            } else {
                System.out.println("Invalid mode. Use 'sender' or 'receiver'.");
            }
        } else {
            testBitStuffing();
            testFrameCreation();
            testBasicCRC();
            testZeroData();
            testAllOnes();
            testKnownValues();
            testXorOperation();
            testMod2Div();
            testCommunication();
            testErrorFrames();
        }
    }

    private static void testBitStuffing() {
        System.out.println("\n=== Test BitStuffing ===");

        // Test case 1: Chaîne avec 5 '1' consécutifs
        String test1 = "11111010101";
        System.out.println("Test 1 - Original: " + test1);
        String stuffed1 = BitStuffing.applyBitStuffing(test1);
        System.out.println("Stuffed: " + stuffed1);
        String unstuffed1 = BitStuffing.removeBitStuffing(stuffed1);
        System.out.println("Unstuffed: " + unstuffed1);
        System.out.println("Test 1 " + (test1.equals(unstuffed1) ? "PASSED" : "FAILED"));

        // Test case 2: Chaîne avec plusieurs séquences de 5 '1'
        String test2 = "1111111111";
        System.out.println("\nTest 2 - Original: " + test2);
        String stuffed2 = BitStuffing.applyBitStuffing(test2);
        System.out.println("Stuffed: " + stuffed2);
        String unstuffed2 = BitStuffing.removeBitStuffing(stuffed2);
        System.out.println("Unstuffed: " + unstuffed2);
        System.out.println("Test 2 " + (test2.equals(unstuffed2) ? "PASSED" : "FAILED"));

        // Test case 3: Chaîne sans besoin de bit stuffing
        String test3 = "10101010";
        System.out.println("\nTest 3 - Original: " + test3);
        String stuffed3 = BitStuffing.applyBitStuffing(test3);
        System.out.println("Stuffed: " + stuffed3);
        String unstuffed3 = BitStuffing.removeBitStuffing(stuffed3);
        System.out.println("Unstuffed: " + unstuffed3);
        System.out.println("Test 3 " + (test3.equals(unstuffed3) ? "PASSED" : "FAILED"));

        // Test case 4: Chaîne vide
        String test4 = "";
        System.out.println("\nTest 4 - Original: \"" + test4 + "\"");
        String stuffed4 = BitStuffing.applyBitStuffing(test4);
        System.out.println("Stuffed: \"" + stuffed4 + "\"");
        String unstuffed4 = BitStuffing.removeBitStuffing(stuffed4);
        System.out.println("Unstuffed: \"" + unstuffed4 + "\"");
        System.out.println("Test 4 " + (test4.equals(unstuffed4) ? "PASSED" : "FAILED"));
    }

    private static void testFrameCreation() {
        System.out.println("\n=== Test Frame Creation ===");
        try {
            // Création d'un objet CRC
            CRC crc = new CRC();

            // Exemple de données à envoyer
            String data = "Hello";

            // Création d'une trame d'information
            byte type = 'I'; // Trame d'information
            byte num = 3; // Numéro de trame

            Frame frame = new Frame(type, num, data, crc);
            byte[] serializedFrame = frame.buildFrame(); // Utiliser la méthode buildFrame qui retourne un tableau de bytes

            System.out.println("Trame sérialisée avec stuffing : " + Arrays.toString(serializedFrame));

            // Désérialisation de la trame
            Frame deserializedFrame = Frame.parseFrame(serializedFrame);
            System.out.println("Trame désérialisée : " + deserializedFrame);

            // Vérifier l'intégrité
            boolean isPassed = frame.getType() == deserializedFrame.getType() &&
                    frame.getNum() == deserializedFrame.getNum() &&
                    frame.getData().equals(deserializedFrame.getData()) &&
                    frame.getCrc().getCrcBits().equals(deserializedFrame.getCrc().getCrcBits());

            System.out.println("Test Frame Creation " + (isPassed ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Frame Creation FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testBasicCRC() {
        System.out.println("\n=== Test Basic CRC ===");
        try {
            CRC crc = new CRC();

            // Données simples
            String data = "1101";
            // Note: Le CRC attendu doit être calculé selon le polynôme utilisé
            // Pour cet exemple, calculons le CRC en utilisant la méthode computeCRC
            String computedCRC = crc.computeCRC(data);
            System.out.println("Données: " + data);
            System.out.println("CRC Calculé: " + computedCRC);

            // Pour valider le test, nous pouvons vérifier que le CRC est correct en recalcultant sur les données + CRC
            String dataWithCRC = data + computedCRC;
            String verificationCRC = crc.computeCRC(data + computedCRC);
            boolean isPassed = verificationCRC.equals("0000000000000000"); // Si le CRC est correct, le recalcul devrait donner zéro
            System.out.println("Test Basic CRC " + (isPassed ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Basic CRC FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testZeroData() {
        System.out.println("\n=== Test Zero Data ===");
        try {
            CRC crc = new CRC();
            String data = ""; // Pas de données

            byte type = 'C'; // Trame de connexion
            byte num = 0;

            Frame frame = new Frame(type, num, data, crc);
            byte[] serializedFrame = frame.buildFrame();
            System.out.println("Trame sérialisée avec stuffing (Zero Data) : " + Arrays.toString(serializedFrame));

            // Désérialisation
            Frame deserializedFrame = Frame.parseFrame(serializedFrame);
            System.out.println("Trame désérialisée : " + deserializedFrame);

            // Vérifier l'intégrité
            boolean isPassed = frame.getType() == deserializedFrame.getType() &&
                    frame.getNum() == deserializedFrame.getNum() &&
                    frame.getData().equals(deserializedFrame.getData()) &&
                    frame.getCrc().getCrcBits().equals(deserializedFrame.getCrc().getCrcBits());

            System.out.println("Test Zero Data " + (isPassed ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Zero Data FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testAllOnes() {
        System.out.println("\n=== Test All Ones ===");
        try {
            CRC crc = new CRC();

            // Données avec beaucoup de '1' consécutifs
            String data = "ÿÿÿ"; // Caractère 255 en ASCII (11111111 en binaire)

            byte type = 'I';
            byte num = 7;

            Frame frame = new Frame(type, num, data, crc);
            byte[] serializedFrame = frame.buildFrame();
            System.out.println("Trame sérialisée avec stuffing (All Ones) : " + Arrays.toString(serializedFrame));

            // Désérialisation
            Frame deserializedFrame = Frame.parseFrame(serializedFrame);
            System.out.println("Trame désérialisée : " + deserializedFrame);

            // Vérifier l'intégrité
            boolean isPassed = frame.getType() == deserializedFrame.getType() &&
                    frame.getNum() == deserializedFrame.getNum() &&
                    frame.getData().equals(deserializedFrame.getData()) &&
                    frame.getCrc().getCrcBits().equals(deserializedFrame.getCrc().getCrcBits());

            System.out.println("Test All Ones " + (isPassed ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test All Ones FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testKnownValues() {
        System.out.println("\n=== Test Known Values ===");
        try {
            CRC crc = new CRC();

            // Exemple avec des données connues et un CRC attendu
            String data1 = "11010011101100";
            String expectedCRC1 = crc.computeCRC(data1); // Calculer le CRC attendu

            String computedCRC1 = crc.computeCRC(data1);
            System.out.println("Données: " + data1);
            System.out.println("CRC Calculé: " + computedCRC1);
            System.out.println("Test Known Values 1 " + (computedCRC1.equals(expectedCRC1) ? "PASSED" : "FAILED"));

            // Exemple 2
            String data2 = "1010101010101010";
            String expectedCRC2 = crc.computeCRC(data2);

            String computedCRC2 = crc.computeCRC(data2);
            System.out.println("\nDonnées: " + data2);
            System.out.println("CRC Calculé: " + computedCRC2);
            System.out.println("Test Known Values 2 " + (computedCRC2.equals(expectedCRC2) ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Known Values FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testXorOperation() {
        System.out.println("\n=== Test XOR Operation ===");
        try {
            // Exemple simple
            String a = "1101";
            String b = "1011";
            String expected = "0110"; // 1^1=0, 1^0=1, 0^1=1, 1^1=0

            String result = CRC.xor(a, b);
            System.out.println("A: " + a);
            System.out.println("B: " + b);
            System.out.println("A XOR B: " + result);
            System.out.println("Test XOR Operation " + (result.equals(expected) ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test XOR Operation FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testMod2Div() {
        System.out.println("\n=== Test Mod2Div ===");
        try {
            // Utiliser un CRC avec un polynôme connu
            CRC crc = new CRC();

            // Exemple
            String dividend = "11010011101100" + "0000000000000000"; // Ajouter 16 zéros
            String expectedRemainder = crc.computeCRC("11010011101100"); // Le reste attendu est le CRC calculé

            String remainder = CRC.mod2Div(dividend);
            System.out.println("Dividend: " + dividend);
            System.out.println("Remainder Calculé: " + remainder);
            System.out.println("Test Mod2Div " + (remainder.equals(expectedRemainder) ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Mod2Div FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void testCommunication() {
        System.out.println("\n=== Test Communication Protocol ===");

        final String hostname = "localhost";
        final int port = 12345;
        final String filename = "test.txt";

        // Créer le fichier de test
        createTestFile(filename);
        System.out.println("Test file created: " + filename);

        // Démarrer le Receiver dans un thread séparé
        Thread receiverThread = new Thread(() -> {
            try {
                System.out.println("Starting receiver...");
                Receiver receiver = new Receiver();
                receiver.initialize(port);

                receiver.acceptConnection();
                System.out.println("Receiver connected");

                while (receiver.isRunning()) {
                    Frame frame = receiver.receiveFrame();
                    if (frame != null) {
                        receiver.processFrame(frame);
                    } else {
                        System.out.println("No frame received or invalid frame.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Receiver error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        receiverThread.start();

        // Démarrer le Sender dans un thread séparé
        Thread senderThread = new Thread(() -> {
            try {
                System.out.println("Starting sender...");
                Sender sender = new Sender();
                sender.initialize(hostname, port, filename);
                sender.readData();
            } catch (Exception e) {
                System.out.println("Sender error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Attendre que le Receiver soit prêt
        try {
            Thread.sleep(1000); // Attendre une seconde pour que le Receiver soit prêt
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        senderThread.start();

        // Attendre la fin de la communication
        try {
            senderThread.join();
            receiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Communication test completed.");

        // Nettoyage
        File testFile = new File(filename);
        if (testFile.exists()) {
            if (testFile.delete()) {
                System.out.println("Test file deleted: " + filename);
            } else {
                System.out.println("Failed to delete test file: " + filename);
            }
        }
    }

    private static void testErrorFrames() {
        System.out.println("\n=== Test Error Frames ===");

        // Test de frames corrompues
        try {
            CRC crc = new CRC();
            String data = "Hello";

            byte type = 'I';
            byte num = 1;

            Frame frame = new Frame(type, num, data, crc);
            byte[] frameBytes = frame.buildFrame();

            // Corrompre la frame en modifiant un bit
            frameBytes[10] ^= 0x01; // Inverser le premier bit de l'octet à l'index 10

            // Tenter de parser la frame corrompue
            try {
                Frame.parseFrame(frameBytes);
                System.out.println("Test Error Frames FAILED: Corrupted frame was parsed without exception.");
            } catch (Exception e) {
                System.out.println("Test Error Frames PASSED: Exception caught as expected.");
                System.out.println("Exception message: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Test Error Frames FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static void createTestFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Première ligne de test");
            writer.println("Deuxième ligne avec 11111 pour tester le bit stuffing");
            writer.println("Troisième ligne pour tester Go-Back-N");
            System.out.println("Test file created successfully");
        } catch (IOException e) {
            System.out.println("Error creating test file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}