package com.hamradio.control;

import java.io.File;
import java.io.IOException;

/**
 * Tests for ScenarioCheckpoint save/load functionality.
 * Creates a ScenarioConfig, saves it to a temp file, loads it back,
 * and verifies all fields match.
 */
public class ScenarioCheckpointTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testSaveAndLoadConfig();
        testSaveAndLoadState();
        testStationFieldsPreserved();
        testMultipleStationsSaveLoad();
        testDefaultValuesOnLoad();

        System.out.println();
        System.out.println("========================================");
        System.out.println("ScenarioCheckpointTest Results: " + passed + " passed, " + failed + " failed");
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

    private static void assertDoubleEqual(double expected, double actual, double tolerance, String testName) {
        if (Math.abs(expected - actual) <= tolerance) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    // --- Tests ---

    /**
     * Save a config with 2 stations, load it back, verify all top-level fields match.
     */
    private static void testSaveAndLoadConfig() {
        System.out.println("--- testSaveAndLoadConfig ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("checkpoint_", ".properties");
            tempFile.deleteOnExit();

            ScenarioConfig original = createTwoStationConfig();
            ScenarioCheckpoint.save(original, ScenarioState.RUNNING, tempFile.getAbsolutePath());

            ScenarioConfig loaded = ScenarioCheckpoint.load(tempFile.getAbsolutePath());

            assertEqual("DX Contest Sim", loaded.getName(), "Loaded name matches");
            assertEqual(48000, loaded.getSampleRate(), "Loaded sample rate matches");
            assertDoubleEqual(120.0, loaded.getDurationSeconds(), 0.001, "Loaded duration matches");
            assertEqual("fspl", loaded.getPropagationModel(), "Loaded propagation model matches");
            assertEqual(2, loaded.getStations().size(), "Loaded station count matches");

        } catch (IOException e) {
            failed++;
            System.out.println("FAIL: testSaveAndLoadConfig threw IOException: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Save with a specific state, load it back, verify the state matches.
     */
    private static void testSaveAndLoadState() {
        System.out.println("--- testSaveAndLoadState ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("checkpoint_state_", ".properties");
            tempFile.deleteOnExit();

            ScenarioConfig config = createTwoStationConfig();

            // Test with RUNNING state
            ScenarioCheckpoint.save(config, ScenarioState.RUNNING, tempFile.getAbsolutePath());
            ScenarioState loadedState = ScenarioCheckpoint.loadState(tempFile.getAbsolutePath());
            assertEqual(ScenarioState.RUNNING, loadedState, "Loaded state is RUNNING");

            // Test with PAUSED state
            ScenarioCheckpoint.save(config, ScenarioState.PAUSED, tempFile.getAbsolutePath());
            loadedState = ScenarioCheckpoint.loadState(tempFile.getAbsolutePath());
            assertEqual(ScenarioState.PAUSED, loadedState, "Loaded state is PAUSED");

            // Test with COMPLETED state
            ScenarioCheckpoint.save(config, ScenarioState.COMPLETED, tempFile.getAbsolutePath());
            loadedState = ScenarioCheckpoint.loadState(tempFile.getAbsolutePath());
            assertEqual(ScenarioState.COMPLETED, loadedState, "Loaded state is COMPLETED");

        } catch (IOException e) {
            failed++;
            System.out.println("FAIL: testSaveAndLoadState threw IOException: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Verify all station-level fields are preserved through save/load.
     */
    private static void testStationFieldsPreserved() {
        System.out.println("--- testStationFieldsPreserved ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("checkpoint_stations_", ".properties");
            tempFile.deleteOnExit();

            ScenarioConfig original = createTwoStationConfig();
            ScenarioCheckpoint.save(original, ScenarioState.RUNNING, tempFile.getAbsolutePath());
            ScenarioConfig loaded = ScenarioCheckpoint.load(tempFile.getAbsolutePath());

            // Station 0: W1AW
            ScenarioConfig.StationConfig s0 = loaded.getStations().get(0);
            assertEqual("W1AW", s0.getCallsign(), "Station 0 callsign matches");
            assertDoubleEqual(41.7147, s0.getLatitude(), 0.001, "Station 0 latitude matches");
            assertDoubleEqual(-72.7272, s0.getLongitude(), 0.001, "Station 0 longitude matches");
            assertDoubleEqual(7100000.0, s0.getFrequencyHz(), 0.1, "Station 0 frequency matches");
            assertEqual("SSB", s0.getMode(), "Station 0 mode matches");
            assertTrue(s0.isUpperSideband(), "Station 0 USB flag matches");

            // Station 1: K2ABC
            ScenarioConfig.StationConfig s1 = loaded.getStations().get(1);
            assertEqual("K2ABC", s1.getCallsign(), "Station 1 callsign matches");
            assertDoubleEqual(40.7128, s1.getLatitude(), 0.001, "Station 1 latitude matches");
            assertDoubleEqual(-74.0060, s1.getLongitude(), 0.001, "Station 1 longitude matches");
            assertDoubleEqual(7100000.0, s1.getFrequencyHz(), 0.1, "Station 1 frequency matches");
            assertEqual("SSB", s1.getMode(), "Station 1 mode matches");
            assertTrue(!s1.isUpperSideband(), "Station 1 LSB flag matches");

        } catch (IOException e) {
            failed++;
            System.out.println("FAIL: testStationFieldsPreserved threw IOException: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Save and load a config with more stations to verify indexing works correctly.
     */
    private static void testMultipleStationsSaveLoad() {
        System.out.println("--- testMultipleStationsSaveLoad ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("checkpoint_multi_", ".properties");
            tempFile.deleteOnExit();

            ScenarioConfig config = new ScenarioConfig("MultiStation");
            config.setSampleRate(22050);
            config.setDurationSeconds(300.0);
            config.setPropagationModel("ionospheric");
            config.addStation(new ScenarioConfig.StationConfig("AA1AA", 42.0, -71.0, 14200000, "AM"));
            config.addStation(new ScenarioConfig.StationConfig("BB2BB", 34.0, -118.0, 14200000, "FM"));
            config.addStation(new ScenarioConfig.StationConfig("CC3CC", 51.5, -0.1, 14200000, "SSB"));
            config.addStation(new ScenarioConfig.StationConfig("DD4DD", 35.7, 139.7, 14200000, "AM"));

            ScenarioCheckpoint.save(config, ScenarioState.VALIDATED, tempFile.getAbsolutePath());
            ScenarioConfig loaded = ScenarioCheckpoint.load(tempFile.getAbsolutePath());

            assertEqual("MultiStation", loaded.getName(), "Multi-station name matches");
            assertEqual(4, loaded.getStations().size(), "Multi-station count matches");
            assertEqual(22050, loaded.getSampleRate(), "Multi-station sample rate matches");
            assertDoubleEqual(300.0, loaded.getDurationSeconds(), 0.001, "Multi-station duration matches");
            assertEqual("ionospheric", loaded.getPropagationModel(), "Multi-station propagation model matches");

            // Verify each station callsign
            assertEqual("AA1AA", loaded.getStations().get(0).getCallsign(), "Station 0 callsign is AA1AA");
            assertEqual("BB2BB", loaded.getStations().get(1).getCallsign(), "Station 1 callsign is BB2BB");
            assertEqual("CC3CC", loaded.getStations().get(2).getCallsign(), "Station 2 callsign is CC3CC");
            assertEqual("DD4DD", loaded.getStations().get(3).getCallsign(), "Station 3 callsign is DD4DD");

            // Verify modes
            assertEqual("AM", loaded.getStations().get(0).getMode(), "Station 0 mode is AM");
            assertEqual("FM", loaded.getStations().get(1).getMode(), "Station 1 mode is FM");
            assertEqual("SSB", loaded.getStations().get(2).getMode(), "Station 2 mode is SSB");
            assertEqual("AM", loaded.getStations().get(3).getMode(), "Station 3 mode is AM");

        } catch (IOException e) {
            failed++;
            System.out.println("FAIL: testMultipleStationsSaveLoad threw IOException: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    /**
     * Verify that loading a minimal (almost empty) checkpoint file uses sensible defaults.
     */
    private static void testDefaultValuesOnLoad() {
        System.out.println("--- testDefaultValuesOnLoad ---");

        File tempFile = null;
        try {
            tempFile = File.createTempFile("checkpoint_defaults_", ".properties");
            tempFile.deleteOnExit();

            // Save a minimal config with no stations
            ScenarioConfig minimal = new ScenarioConfig("Minimal");
            ScenarioCheckpoint.save(minimal, ScenarioState.DRAFT, tempFile.getAbsolutePath());

            ScenarioConfig loaded = ScenarioCheckpoint.load(tempFile.getAbsolutePath());
            assertEqual("Minimal", loaded.getName(), "Minimal config name is Minimal");
            assertEqual(0, loaded.getStations().size(), "Minimal config has 0 stations");

            ScenarioState state = ScenarioCheckpoint.loadState(tempFile.getAbsolutePath());
            assertEqual(ScenarioState.DRAFT, state, "Minimal config state is DRAFT");

        } catch (IOException e) {
            failed++;
            System.out.println("FAIL: testDefaultValuesOnLoad threw IOException: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    // --- Factory ---

    private static ScenarioConfig createTwoStationConfig() {
        ScenarioConfig config = new ScenarioConfig("DX Contest Sim");
        config.setSampleRate(48000);
        config.setDurationSeconds(120.0);
        config.setPropagationModel("fspl");

        ScenarioConfig.StationConfig station1 = new ScenarioConfig.StationConfig(
                "W1AW", 41.7147, -72.7272, 7100000, "SSB");
        station1.setUpperSideband(true);
        config.addStation(station1);

        ScenarioConfig.StationConfig station2 = new ScenarioConfig.StationConfig(
                "K2ABC", 40.7128, -74.0060, 7100000, "SSB");
        station2.setUpperSideband(false);
        config.addStation(station2);

        return config;
    }
}
