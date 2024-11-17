import java.io.*;
import java.net.*;

public class MainTest {
    public static void main(String[] args) {
        testBitStuffing();
        testFrameCreation();
        testBasicCRC();
        testZeroData();
        testAllOnes();
        testKnownValues();
        testXorOperation();
        testMod2Div();
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
    }

    private static void testFrameCreation() {
        System.out.println("\n=== Test Frame Creation ===");
        CRC crc = new CRC();

        // Test case 1: Frame normale
        Frame frame1 = new Frame((byte)'I', '1', "10101010", crc);
        System.out.println("\nTest Frame 1 - Before Bit Stuffing:");
        System.out.println(frame1);
        frame1.applyBitStuffing();
        System.out.println("After Bit Stuffing:");
        System.out.println(frame1);
        frame1.removeBitStuffing();
        System.out.println("After Removing Bit Stuffing:");
        System.out.println(frame1);

        // Test case 2: Frame avec une séquence nécessitant bit stuffing
        Frame frame2 = new Frame((byte)'I', '2', "11111010", crc);
        System.out.println("\nTest Frame 2 - Before Bit Stuffing:");
        System.out.println(frame2);
        frame2.applyBitStuffing();
        System.out.println("After Bit Stuffing:");
        System.out.println(frame2);
        frame2.removeBitStuffing();
        System.out.println("After Removing Bit Stuffing:");
        System.out.println(frame2);

        // Test case 3: Frame de connexion
        Frame frame3 = new Frame((byte)'C', "00000000", crc);
        System.out.println("\nTest Frame 3 - Connection Frame:");
        System.out.println(frame3);
        
        // Test case 4: Test buildFrameFromBytes
        Frame frame4 = new Frame((byte)'I', '3', "11111111", crc);
        System.out.println("\nTest Frame 4 - Complete Frame:");
        System.out.println("Frame as binary string:");
        System.out.println(frame4.buildFrameFromBytes());
    }
    private static void testBasicCRC() {
        System.out.println("\n=== Test Basic CRC ===");
        CRC crc = new CRC();
        
        // Test avec une chaîne simple
        String data1 = "1010";
        String result1 = crc.computeCRC(data1);
        System.out.println("Test 1 - Data: " + data1);
        System.out.println("CRC: " + result1);
        System.out.println("CRC Length: " + result1.length() + " (should be 16)");
        System.out.println("Test 1 " + (result1.length() == 16 ? "PASSED" : "FAILED"));
        
        // Test avec une chaîne plus longue
        String data2 = "10101010101010101";
        String result2 = crc.computeCRC(data2);
        System.out.println("\nTest 2 - Data: " + data2);
        System.out.println("CRC: " + result2);
        System.out.println("Test 2 " + (result2.length() == 16 ? "PASSED" : "FAILED"));
    }

    private static void testZeroData() {
        System.out.println("\n=== Test Zero Data ===");
        CRC crc = new CRC();
        
        // Test avec une chaîne de zéros
        String data = "0000000";
        String result = crc.computeCRC(data);
        System.out.println("Data: " + data);
        System.out.println("CRC: " + result);
        System.out.println("Test " + (result.length() == 16 ? "PASSED" : "FAILED"));
    }

    private static void testAllOnes() {
        System.out.println("\n=== Test All Ones ===");
        CRC crc = new CRC();
        
        // Test avec une chaîne de uns
        String data = "11111111";
        String result = crc.computeCRC(data);
        System.out.println("Data: " + data);
        System.out.println("CRC: " + result);
        System.out.println("Test " + (result.length() == 16 ? "PASSED" : "FAILED"));
    }

    private static void testKnownValues() {
        System.out.println("\n=== Test Known Values ===");
        CRC crc = new CRC();
        
        // Test avec des valeurs connues
        String[][] testCases = {
            {"1100", "1100110011001100"},  
            {"1010", "1010101010101010"},  
            {"1111", "1111000011110000"}   
        };

        for (int i = 0; i < testCases.length; i++) {
            String data = testCases[i][0];
            String expectedCRC = testCases[i][1];
            String result = crc.computeCRC(data);
            System.out.println("\nTest " + (i + 1) + " - Data: " + data);
            System.out.println("Expected CRC: " + expectedCRC);
            System.out.println("Actual CRC:   " + result);
            System.out.println("Test " + (i + 1) + " " + 
                (result.length() == 16 ? "PASSED" : "FAILED (wrong length)"));
        }
    }

    private static void testXorOperation() {
        System.out.println("\n=== Test XOR Operation ===");
        
        // Test de l'opération XOR avec des cas connus
        String a = "1010";
        String b = "1100";
        String expected = "0110";
        String result = CRC.Xor(a, b);
        
        System.out.println("XOR Test:");
        System.out.println("a:        " + a);
        System.out.println("b:        " + b);
        System.out.println("Expected: " + expected);
        System.out.println("Result:   " + result);
        System.out.println("Test " + (result.equals(expected) ? "PASSED" : "FAILED"));
    }

    private static void testMod2Div() {
        System.out.println("\n=== Test Mod2Div Operation ===");
        
        // Il faut que le dividend soit plus long que le diviseur (17 bits)
        // Ajoutons les 16 bits de zéros nécessaires pour le CRC
        String dividend = "11010101" + "0000000000000000";  // Ajout de 16 zéros
        String result = CRC.Mod2Div(dividend);
        
        System.out.println("Mod2Div Test:");
        System.out.println("Original data: 11010101");
        System.out.println("Padded dividend: " + dividend);
        System.out.println("Result: " + result);
        System.out.println("Result length: " + result.length());
        System.out.println("Test " + (result.length() == 16 ? "PASSED" : "FAILED"));

        CRC crc = new CRC();
        String crcResult = crc.computeCRC("11010101");
        System.out.println("\nComparison with computeCRC:");
        System.out.println("Mod2Div result:    " + result);
        System.out.println("ComputeCRC result: " + crcResult);
        System.out.println("Comparison test " + 
            (result.equals(crcResult) ? "PASSED" : "FAILED"));
    }
}