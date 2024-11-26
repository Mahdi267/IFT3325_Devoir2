import java.io.*;
import java.net.*;
import java.util.Arrays;

public class MainTest {
    public static void main(String[] args){
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
            String dataBinary = stringToBinary(data); // Convertir "Hello" en binaire

            // Création d'une trame d'information
            byte type = 'I'; // Trame d'information
            byte num = 3; // Numéro de trame

            Frame frame = new Frame(type, num, dataBinary, crc);
            String serializedFrame = frame.buildFrameFromBytes();
            System.out.println("Trame sérialisée avec bit stuffing : " + serializedFrame);

            // Désérialisation de la trame
            Frame deserializedFrame = Frame.unBuildFrame(serializedFrame);
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
            String expectedCRC = "1101000110101101"; // CRC calculé manuellement pour ce petit exemple

            String computedCRC = crc.computeCRC(data);
            System.out.println("Données: " + data);
            System.out.println("CRC Calculé: " + computedCRC);
            System.out.println("Test Basic CRC " + (computedCRC.equals(expectedCRC) ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Basic CRC FAILED with exception:");
            e.printStackTrace();
        };
    }

    private static void testZeroData() {
        System.out.println("\n=== Test Zero Data ===");
        try {
            CRC crc = new CRC();
            String data = ""; // Pas de données
            String dataBinary = stringToBinary(data);

            byte type = 'C'; // Trame de connexion
            byte num = 0;

            Frame frame = new Frame(type, num, dataBinary, crc);
            String serializedFrame = frame.buildFrameFromBytes();
            System.out.println("Trame sérialisée avec bit stuffing (Zero Data) : " + serializedFrame);

            // Désérialisation
            Frame deserializedFrame = Frame.unBuildFrame(serializedFrame);
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
            String dataBinary = "1111111"; // Données déjà en binaire

            byte type = 'I';
            byte num = 7;

            Frame frame = new Frame(type, num, dataBinary, crc);
            String serializedFrame = frame.buildFrameFromBytes();
            System.out.println("Trame sérialisée avec bit stuffing (All Ones) : " + serializedFrame);

            // Désérialisation
            Frame deserializedFrame = Frame.unBuildFrame(serializedFrame);
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

            // Exemple 1
            String data1 = "11010011101100";
            String expectedCRC1 = "1111010111110011"; // CRC attendu à définir selon le polynôme

            String computedCRC1 = crc.computeCRC(data1);
            System.out.println("Données: " + data1);
            System.out.println("CRC Calculé: " + computedCRC1);
            System.out.println("Test Known Values 1 " + (computedCRC1.equals(expectedCRC1) ? "PASSED" : "FAILED"));

            // Exemple 2
            String data2 = "1010101010101010";
            String expectedCRC2 = "1110011000010101"; // CRC attendu à définir

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

            String result = CRC.Xor(a, b);
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

            // Exemple 1
            String dividend1 = "11010011101100" + "0000000000000000"; // Ajouter 16 zéros
            String expectedRemainder1 = "1111010111110011"; // À définir selon le polynôme

            String remainder1 = CRC.Mod2Div(dividend1);
            System.out.println("Dividend: " + dividend1);
            System.out.println("Remainder Calculé: " + remainder1);
            System.out.println("Test Mod2Div 1 " + (remainder1.equals(expectedRemainder1) ? "PASSED" : "FAILED"));

            // Exemple 2
            String dividend2 = "1010101010101010" + "0000000000000000"; // Ajouter 16 zéros
            String expectedRemainder2 = "1110011000010101"; // À définir selon le polynôme

            String remainder2 = CRC.Mod2Div(dividend2);
            System.out.println("\nDividend: " + dividend2);
            System.out.println("Remainder Calculé: " + remainder2);
            System.out.println("Test Mod2Div 2 " + (remainder2.equals(expectedRemainder2) ? "PASSED" : "FAILED"));
        } catch (Exception e) {
            System.out.println("Test Mod2Div FAILED with exception:");
            e.printStackTrace();
        }
    }

    private static String stringToBinary(String input) {
        StringBuilder binary = new StringBuilder();
        for (char c : input.toCharArray()) {
            binary.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binary.toString();
    }
    private static volatile boolean receiverReady = false;
    private static volatile boolean communicationCompleted = false;

    public static void testCommunication() {
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
                receiverReady = true;

                System.out.println("Receiver waiting for connection...");
                receiver.acceptConnection();
                System.out.println("Receiver connected");

                while (true) {
                    Frame frame = receiver.receiveFrame();
                    if (frame == null) continue;

                    receiver.processFrame(frame);

                    if (frame.getType() == 'F') {
                        System.out.println("End of transmission received");
                        communicationCompleted = true;
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Receiver error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        receiverThread.start();

        // Attendre que le receiver soit prêt
        while (!receiverReady) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

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
        senderThread.start();

        // Attendre la fin de la communication avec timeout
        long startTime = System.currentTimeMillis();
        long timeout = 15000; // 15 seconds timeout

        while (!communicationCompleted && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!communicationCompleted) {
            System.out.println("WARNING: Communication test timed out after 15 seconds");
        } else {
            System.out.println("Communication test completed successfully");
        }

        // Nettoyage
        try {
            senderThread.join(1000);
            receiverThread.join(1000);
            File testFile = new File(filename);
            if (testFile.exists()) {
                if (testFile.delete()) {
                    System.out.println("Test file deleted: " + filename);
                } else {
                    System.out.println("Failed to delete test file: " + filename);
                }
            }
            System.out.println("Test cleanup completed");
        } catch (InterruptedException e) {
            System.out.println("Cleanup was interrupted");
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