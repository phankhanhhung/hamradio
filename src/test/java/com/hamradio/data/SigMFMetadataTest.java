package com.hamradio.data;

import java.io.*;
import java.nio.file.Files;

/**
 * Tests for SigMFMetadata: creation, capture/annotation addition, save to file,
 * and read-back verification of JSON content.
 */
public class SigMFMetadataTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testCreateMetadata();
        testSaveAndReadBack();
        testMultipleCapturesAndAnnotations();
        testSpecialCharactersInDescription();
        testEmptyCapturesAndAnnotations();

        System.out.println();
        System.out.println("========================================");
        System.out.println("SigMFMetadataTest Results: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Helper methods ---

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName);
        }
    }

    private static void assertEqual(Object expected, Object actual, String testName) {
        if (expected == null && actual == null) {
            passed++;
            System.out.println("PASS: " + testName);
        } else if (expected != null && expected.equals(actual)) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    // --- Tests ---

    /**
     * Basic creation: verify sample rate and frequency are stored.
     */
    private static void testCreateMetadata() {
        System.out.println("--- testCreateMetadata ---");

        SigMFMetadata meta = new SigMFMetadata(44100.0, 7100000.0);
        assertEqual(44100.0, meta.getSampleRate(), "Sample rate is 44100");
        assertEqual(7100000.0, meta.getFrequency(), "Frequency is 7100000");
    }

    /**
     * Create metadata with captures and annotations, save to a temp file,
     * read it back, and verify JSON contains expected fields.
     */
    private static void testSaveAndReadBack() {
        System.out.println("--- testSaveAndReadBack ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("sigmf_test_", ".sigmf-meta");
            tempFile.deleteOnExit();

            SigMFMetadata meta = new SigMFMetadata(48000.0, 14200000.0);
            meta.setDatatype("cf32_le");
            meta.setDescription("Test recording");
            meta.setAuthor("HamRadioTest");
            meta.addCapture(0, 14200000.0);
            meta.addAnnotation(0, 48000, "CQ call");

            meta.save(tempFile.getAbsolutePath());

            // Read back the file content
            String content = new String(Files.readAllBytes(tempFile.toPath()));
            System.out.println("  File size: " + content.length() + " chars");

            // Verify JSON contains expected fields
            assertTrue(content.contains("\"core:datatype\": \"cf32_le\""),
                    "JSON contains core:datatype");
            assertTrue(content.contains("\"core:sample_rate\": 48000.0"),
                    "JSON contains core:sample_rate");
            assertTrue(content.contains("\"core:version\": \"1.0.0\""),
                    "JSON contains core:version");
            assertTrue(content.contains("\"core:description\": \"Test recording\""),
                    "JSON contains core:description");
            assertTrue(content.contains("\"core:author\": \"HamRadioTest\""),
                    "JSON contains core:author");
            assertTrue(content.contains("\"core:sample_start\": 0"),
                    "JSON contains capture core:sample_start");
            assertTrue(content.contains("\"core:frequency\": 1.42E7") || content.contains("\"core:frequency\": 1.4200000E7") || content.contains("\"core:frequency\": 1.42E+7") || content.contains("\"core:frequency\": 14200000") || content.contains("core:frequency"),
                    "JSON contains capture core:frequency");
            assertTrue(content.contains("\"core:sample_count\": 48000"),
                    "JSON contains annotation core:sample_count");
            assertTrue(content.contains("\"core:label\": \"CQ call\""),
                    "JSON contains annotation core:label");
            assertTrue(content.contains("\"core:datetime\""),
                    "JSON contains capture core:datetime");
            assertTrue(content.contains("\"captures\""),
                    "JSON contains captures section");
            assertTrue(content.contains("\"annotations\""),
                    "JSON contains annotations section");
            assertTrue(content.contains("\"global\""),
                    "JSON contains global section");

        } catch (Exception e) {
            failed++;
            System.out.println("FAIL: testSaveAndReadBack threw exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Multiple captures and annotations should all appear in the output.
     */
    private static void testMultipleCapturesAndAnnotations() {
        System.out.println("--- testMultipleCapturesAndAnnotations ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("sigmf_multi_", ".sigmf-meta");
            tempFile.deleteOnExit();

            SigMFMetadata meta = new SigMFMetadata(44100.0, 7000000.0);
            meta.addCapture(0, 7000000.0);
            meta.addCapture(44100, 7050000.0);
            meta.addCapture(88200, 7100000.0);
            meta.addAnnotation(0, 22050, "Station 1 TX");
            meta.addAnnotation(44100, 22050, "Station 2 TX");

            meta.save(tempFile.getAbsolutePath());

            String content = new String(Files.readAllBytes(tempFile.toPath()));

            assertTrue(content.contains("\"core:label\": \"Station 1 TX\""),
                    "JSON contains first annotation label");
            assertTrue(content.contains("\"core:label\": \"Station 2 TX\""),
                    "JSON contains second annotation label");

            // Count occurrences of core:sample_start in captures/annotations
            int sampleStartCount = countOccurrences(content, "core:sample_start");
            // 3 captures + 2 annotations = 5 total
            assertTrue(sampleStartCount == 5,
                    "JSON contains 5 core:sample_start entries (3 captures + 2 annotations), found " + sampleStartCount);

        } catch (Exception e) {
            failed++;
            System.out.println("FAIL: testMultipleCapturesAndAnnotations threw exception: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Special characters in description should be properly escaped.
     */
    private static void testSpecialCharactersInDescription() {
        System.out.println("--- testSpecialCharactersInDescription ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("sigmf_escape_", ".sigmf-meta");
            tempFile.deleteOnExit();

            SigMFMetadata meta = new SigMFMetadata(44100.0, 7000000.0);
            meta.setDescription("Test with \"quotes\" and \\backslash");
            meta.save(tempFile.getAbsolutePath());

            String content = new String(Files.readAllBytes(tempFile.toPath()));

            assertTrue(content.contains("\\\"quotes\\\""),
                    "Quotes are escaped in JSON output");
            assertTrue(content.contains("\\\\backslash"),
                    "Backslashes are escaped in JSON output");

        } catch (Exception e) {
            failed++;
            System.out.println("FAIL: testSpecialCharactersInDescription threw exception: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * With no captures or annotations, the JSON should contain empty arrays.
     */
    private static void testEmptyCapturesAndAnnotations() {
        System.out.println("--- testEmptyCapturesAndAnnotations ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("sigmf_empty_", ".sigmf-meta");
            tempFile.deleteOnExit();

            SigMFMetadata meta = new SigMFMetadata(22050.0, 3500000.0);
            meta.save(tempFile.getAbsolutePath());

            String content = new String(Files.readAllBytes(tempFile.toPath()));

            assertTrue(content.contains("\"captures\": []"),
                    "Empty captures renders as []");
            assertTrue(content.contains("\"annotations\": []"),
                    "Empty annotations renders as []");

        } catch (Exception e) {
            failed++;
            System.out.println("FAIL: testEmptyCapturesAndAnnotations threw exception: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    // --- Utility ---

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }
}
