package com.hamradio.control;

import com.hamradio.event.EventBus;

/**
 * Tests for ScenarioManager state transitions.
 * Verifies the full lifecycle: DRAFT -> VALIDATED -> LOADING -> RUNNING -> PAUSED -> RUNNING -> COMPLETED
 * Also verifies that invalid transitions throw IllegalStateException and that
 * validation fails when fewer than 2 stations are configured.
 */
public class ScenarioManagerTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testFullLifecycleTransition();
        testValidateFromRunningThrows();
        testLoadFromDraftThrows();
        testPauseFromDraftThrows();
        testResumeFromRunningThrows();
        testCompleteFromDraftThrows();
        testValidationFailsWithLessThanTwoStations();
        testValidationFailsWithOneStation();
        testValidationFailsWithNoName();
        testCompleteFromPaused();

        System.out.println();
        System.out.println("========================================");
        System.out.println("ScenarioManagerTest Results: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Helper methods ---

    private static ScenarioManager createManager() {
        EventBus eventBus = EventBus.getInstance();
        ResourceAllocator allocator = new ResourceAllocator(1);
        return new ScenarioManager(eventBus, allocator);
    }

    private static ScenarioConfig createValidConfig() {
        ScenarioConfig config = new ScenarioConfig("TestScenario");
        config.addStation(new ScenarioConfig.StationConfig("W1AW", 41.71, -72.73, 7100000, "SSB"));
        config.addStation(new ScenarioConfig.StationConfig("K2ABC", 40.71, -74.01, 7100000, "SSB"));
        return config;
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

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName);
        }
    }

    // --- Tests ---

    private static void testFullLifecycleTransition() {
        System.out.println("--- testFullLifecycleTransition ---");
        ScenarioManager mgr = createManager();

        assertEqual(ScenarioState.DRAFT, mgr.getState(), "Initial state is DRAFT");

        mgr.setConfig(createValidConfig());
        mgr.validate();
        assertEqual(ScenarioState.VALIDATED, mgr.getState(), "After validate: VALIDATED");

        mgr.load();
        // load() transitions through LOADING -> RUNNING internally
        assertEqual(ScenarioState.RUNNING, mgr.getState(), "After load: RUNNING");

        mgr.pause();
        assertEqual(ScenarioState.PAUSED, mgr.getState(), "After pause: PAUSED");

        mgr.resume();
        assertEqual(ScenarioState.RUNNING, mgr.getState(), "After resume: RUNNING");

        mgr.complete();
        assertEqual(ScenarioState.COMPLETED, mgr.getState(), "After complete: COMPLETED");
    }

    private static void testValidateFromRunningThrows() {
        System.out.println("--- testValidateFromRunningThrows ---");
        ScenarioManager mgr = createManager();
        mgr.setConfig(createValidConfig());
        mgr.validate();
        mgr.load();

        boolean threw = false;
        try {
            mgr.validate(); // should fail: state is RUNNING, not DRAFT
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "validate() from RUNNING throws IllegalStateException");
    }

    private static void testLoadFromDraftThrows() {
        System.out.println("--- testLoadFromDraftThrows ---");
        ScenarioManager mgr = createManager();
        mgr.setConfig(createValidConfig());

        boolean threw = false;
        try {
            mgr.load(); // should fail: state is DRAFT, not VALIDATED
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "load() from DRAFT throws IllegalStateException");
    }

    private static void testPauseFromDraftThrows() {
        System.out.println("--- testPauseFromDraftThrows ---");
        ScenarioManager mgr = createManager();

        boolean threw = false;
        try {
            mgr.pause(); // should fail: state is DRAFT, not RUNNING
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "pause() from DRAFT throws IllegalStateException");
    }

    private static void testResumeFromRunningThrows() {
        System.out.println("--- testResumeFromRunningThrows ---");
        ScenarioManager mgr = createManager();
        mgr.setConfig(createValidConfig());
        mgr.validate();
        mgr.load();

        boolean threw = false;
        try {
            mgr.resume(); // should fail: state is RUNNING, not PAUSED
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "resume() from RUNNING throws IllegalStateException");
    }

    private static void testCompleteFromDraftThrows() {
        System.out.println("--- testCompleteFromDraftThrows ---");
        ScenarioManager mgr = createManager();

        boolean threw = false;
        try {
            mgr.complete(); // should fail: state is DRAFT
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "complete() from DRAFT throws IllegalStateException");
    }

    private static void testValidationFailsWithLessThanTwoStations() {
        System.out.println("--- testValidationFailsWithLessThanTwoStations ---");
        ScenarioManager mgr = createManager();
        ScenarioConfig config = new ScenarioConfig("EmptyScenario");
        // No stations added
        mgr.setConfig(config);

        boolean threw = false;
        try {
            mgr.validate();
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue(e.getMessage().contains("At least 2 stations"),
                    "Error message mentions station requirement");
        }
        assertTrue(threw, "validate() with 0 stations throws IllegalStateException");
    }

    private static void testValidationFailsWithOneStation() {
        System.out.println("--- testValidationFailsWithOneStation ---");
        // Need a fresh manager since previous one transitioned to FAILED
        ScenarioManager mgr = createManager();
        ScenarioConfig config = new ScenarioConfig("OneStationScenario");
        config.addStation(new ScenarioConfig.StationConfig("W1AW", 41.71, -72.73, 7100000, "SSB"));
        mgr.setConfig(config);

        boolean threw = false;
        try {
            mgr.validate();
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "validate() with 1 station throws IllegalStateException");
    }

    private static void testValidationFailsWithNoName() {
        System.out.println("--- testValidationFailsWithNoName ---");
        ScenarioManager mgr = createManager();
        ScenarioConfig config = new ScenarioConfig("");
        config.addStation(new ScenarioConfig.StationConfig("W1AW", 41.71, -72.73, 7100000, "SSB"));
        config.addStation(new ScenarioConfig.StationConfig("K2ABC", 40.71, -74.01, 7100000, "SSB"));
        mgr.setConfig(config);

        boolean threw = false;
        try {
            mgr.validate();
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue(e.getMessage().contains("name"),
                    "Error message mentions scenario name requirement");
        }
        assertTrue(threw, "validate() with empty name throws IllegalStateException");
    }

    private static void testCompleteFromPaused() {
        System.out.println("--- testCompleteFromPaused ---");
        ScenarioManager mgr = createManager();
        mgr.setConfig(createValidConfig());
        mgr.validate();
        mgr.load();
        mgr.pause();

        assertEqual(ScenarioState.PAUSED, mgr.getState(), "State is PAUSED before complete");

        mgr.complete(); // should succeed: complete() allows RUNNING or PAUSED
        assertEqual(ScenarioState.COMPLETED, mgr.getState(), "complete() from PAUSED succeeds");
    }
}
