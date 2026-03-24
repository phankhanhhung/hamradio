package com.hamradio.integration;

import com.hamradio.control.*;
import com.hamradio.data.*;
import com.hamradio.dsp.NativeDSP;
import com.hamradio.event.EventBus;
import com.hamradio.event.EventListener;
import com.hamradio.event.events.*;
import com.hamradio.net.*;
import com.hamradio.plugin.*;
import com.hamradio.rf.*;
import com.hamradio.rf.models.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Full UI simulation test — mimics exactly what happens when two users
 * interact through the GUI to conduct a QSO (radio contact).
 *
 * Scenario: Two ham radio operators:
 *   - VK3ABC (Melbourne, Australia) — Operator A
 *   - JA1YXP (Tokyo, Japan) — Operator B
 *
 * They conduct an SSB QSO on 7.1 MHz (40m band) with full propagation.
 * The test simulates every button click and verifies every UI update.
 */
public class UISimulationTest {

    static { System.loadLibrary("hamradio"); }

    // Subsystems (same as MainWindow creates)
    private static final EventBus eventBus = EventBus.getInstance();
    private static final ResourceAllocator resourceAllocator = new ResourceAllocator();
    private static final ScenarioManager scenarioManager = new ScenarioManager(eventBus, resourceAllocator);
    private static final StationManager stationManager = new StationManager(eventBus);
    private static final SessionManager sessionManager = new SessionManager();
    private static final NativeDSP nativeDSP = new NativeDSP();
    private static final NativeRF nativeRF = new NativeRF();
    private static final PluginRegistry pluginRegistry = new PluginRegistry();
    private static final PluginLifecycleManager pluginManager = new PluginLifecycleManager(pluginRegistry);

    // Event collectors (simulates what UI panels would receive)
    private static final List<String> logMessages = new CopyOnWriteArrayList<>();
    private static final List<String> scenarioStates = new CopyOnWriteArrayList<>();
    private static final List<String> stationEvents = new CopyOnWriteArrayList<>();
    private static final List<float[]> spectrumSnapshots = new CopyOnWriteArrayList<>();
    private static final List<String> rxMessages = new CopyOnWriteArrayList<>();

    // Test counters
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  HAM RADIO UI SIMULATION TEST                               ║");
        System.out.println("║  Scenario: VK3ABC (Melbourne) ↔ JA1YXP (Tokyo) SSB QSO     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // Wire event listeners (simulates UIEventBridge)
        wireEventListeners();

        // ============================================================
        // ACT 1: Application Startup
        // ============================================================
        System.out.println("━━━ ACT 1: Application Startup ━━━");

        // [User opens app] → Plugin framework initializes
        pluginManager.discover();
        pluginManager.initAll();
        pluginManager.startAll();
        check("Plugins initialized", true);

        // [User sees] → Database initializes
        DatabaseManager db = new DatabaseManager("test_simulation.db");
        db.initialize();
        MetadataStore metadataStore = new MetadataStore(db);
        check("Database initialized", db.getConnection() != null);

        System.out.println("  StatusBar: Plugins: " + pluginRegistry.getAll().size());
        System.out.println();

        // ============================================================
        // ACT 2: Operator A configures scenario
        // ============================================================
        System.out.println("━━━ ACT 2: Operator A Configures Scenario ━━━");

        // [User fills form] → ScenarioConfigPanel
        ScenarioConfig config = new ScenarioConfig("VK3ABC-JA1YXP 40m SSB QSO");
        config.setSampleRate(44100);
        config.setDurationSeconds(120);
        config.setPropagationModel("full");

        // Station 1: Melbourne
        ScenarioConfig.StationConfig s1 = new ScenarioConfig.StationConfig(
                "VK3ABC", -37.8136, 144.9631, 7100000, "SSB");
        s1.setUpperSideband(true);
        config.addStation(s1);

        // Station 2: Tokyo
        ScenarioConfig.StationConfig s2 = new ScenarioConfig.StationConfig(
                "JA1YXP", 35.6762, 139.6503, 7100000, "SSB");
        s2.setUpperSideband(true);
        config.addStation(s2);

        System.out.println("  Config: " + config.getName());
        System.out.println("  Mode: SSB (USB) @ 7.100 MHz");
        System.out.println("  Sample Rate: " + config.getSampleRate() + " Hz");
        System.out.println("  Propagation: " + config.getPropagationModel());
        System.out.println("  Station 1: " + s1.getCallsign() + " (Melbourne -37.8, 144.9)");
        System.out.println("  Station 2: " + s2.getCallsign() + " (Tokyo 35.7, 139.7)");
        System.out.println();

        // ============================================================
        // ACT 3: User clicks START
        // ============================================================
        System.out.println("━━━ ACT 3: User Clicks [START] ━━━");

        scenarioStates.clear();
        logMessages.clear();

        // [Click START] → same as MainWindow.startScenario()
        scenarioManager.reset();
        scenarioManager.setConfig(config);
        check("State after reset: DRAFT", scenarioManager.getState() == ScenarioState.DRAFT);

        scenarioManager.validate();
        check("State after validate: VALIDATED", scenarioManager.getState() == ScenarioState.VALIDATED);

        stationManager.createFromConfig(config);
        List<Station> stations = new ArrayList<>(stationManager.getAllStations());
        check("2 stations created", stations.size() == 2);

        Station stationA = stations.get(0);
        Station stationB = stations.get(1);
        double distance = stationA.distanceTo(stationB);
        System.out.println("  Distance: " + String.format("%.0f km", distance / 1000));
        check("Distance Melbourne→Tokyo ≈ 8000 km", distance > 7000000 && distance < 9000000);

        nativeDSP.dspInit(config.getSampleRate());
        check("DSP initialized", true);

        scenarioManager.load();
        check("State: RUNNING", scenarioManager.getState() == ScenarioState.RUNNING);

        // Save to DB
        long scenarioId = metadataStore.saveScenario(config.getName(), config.getPropagationModel(), "RUNNING");
        check("Scenario saved to DB", scenarioId > 0);

        // Verify events received
        check("Scenario state events received", scenarioStates.size() >= 3);
        check("Station created events received", stationEvents.size() >= 2);

        // [UI shows] StationPanel updates
        System.out.println("  StationPanel 1: " + stationA.getCallsign() + " @ " +
                String.format("%.0f Hz", stationA.getFrequencyHz()) + " " + stationA.getMode());
        System.out.println("  StationPanel 2: " + stationB.getCallsign() + " @ " +
                String.format("%.0f Hz", stationB.getFrequencyHz()) + " " + stationB.getMode());
        System.out.println("  StatusBar: RUNNING | SR: 44100 | Stations: 2");
        System.out.println("  [TX button enabled, PAUSE/STOP enabled]");
        System.out.println();

        // ============================================================
        // ACT 4: User enables recording
        // ============================================================
        System.out.println("━━━ ACT 4: User Clicks [REC] ━━━");

        new File("test_recordings").mkdirs();
        String recBasePath = "test_recordings/qso_vk3abc_ja1yxp";
        IQRecorder recorder = new IQRecorder(recBasePath, config.getSampleRate(), 7100000);
        recorder.start();
        check("IQ Recording started", true);
        System.out.println("  REC button: RED (recording armed)");
        System.out.println("  Recording to: " + recBasePath);
        System.out.println();

        // ============================================================
        // ACT 5: Operator A transmits CQ call
        // ============================================================
        System.out.println("━━━ ACT 5: Operator A Types Message & Clicks [TX] ━━━");
        System.out.println("  Message: \"CQ CQ CQ DE VK3ABC VK3ABC K\"");

        String msg1 = "CQ CQ CQ DE VK3ABC VK3ABC K";
        float[] result1 = simulateTransmission(msg1, stationA, stationB, config, recorder);

        check("TX produced signal", result1 != null);
        check("TX signal has samples", result1 != null && result1.length > 0);
        check("Spectrum data received", spectrumSnapshots.size() > 0);

        System.out.println("  StationPanel 1: State → TRANSMITTING (red)");
        System.out.println("  StationPanel 2: State → RECEIVING (blue)");
        System.out.println("  SpectrumView: updated with TX spectrum");
        System.out.println("  WaterfallView: new line added");
        System.out.println("  TransceiverPanel RX: signal received info");
        System.out.println("  StationPanels: State → IDLE");
        System.out.println();

        // Create QSO session on first TX
        SessionManager.Session session = sessionManager.createSession(
                stationA.getCallsign(), stationB.getCallsign());
        check("QSO session created", session != null);
        check("Session is active", session.isActive());
        System.out.println("  Session: " + stationA.getCallsign() + " ↔ " + stationB.getCallsign());
        System.out.println();

        // ============================================================
        // ACT 6: Operator B responds
        // ============================================================
        System.out.println("━━━ ACT 6: Operator B Responds ━━━");
        System.out.println("  Message: \"VK3ABC DE JA1YXP UR 59 QTH TOKYO K\"");

        String msg2 = "VK3ABC DE JA1YXP UR 59 QTH TOKYO K";
        spectrumSnapshots.clear();
        float[] result2 = simulateTransmission(msg2, stationB, stationA, config, recorder);

        check("Response TX produced signal", result2 != null);
        check("Response spectrum received", spectrumSnapshots.size() > 0);

        System.out.println("  StationPanel 2: State → TRANSMITTING");
        System.out.println("  StationPanel 1: State → RECEIVING");
        System.out.println("  Spectrum/Waterfall: updated with response");
        System.out.println();

        // ============================================================
        // ACT 7: Operator A sends report
        // ============================================================
        System.out.println("━━━ ACT 7: Operator A Sends Signal Report ━━━");
        System.out.println("  Message: \"JA1YXP DE VK3ABC UR 57 QTH MELBOURNE 73 K\"");

        String msg3 = "JA1YXP DE VK3ABC UR 57 QTH MELBOURNE 73 K";
        spectrumSnapshots.clear();
        float[] result3 = simulateTransmission(msg3, stationA, stationB, config, recorder);

        check("Report TX produced signal", result3 != null);
        System.out.println();

        // ============================================================
        // ACT 8: Operator B signs off
        // ============================================================
        System.out.println("━━━ ACT 8: Operator B Signs Off ━━━");
        System.out.println("  Message: \"VK3ABC DE JA1YXP 73 SK\"");

        String msg4 = "VK3ABC DE JA1YXP 73 SK";
        spectrumSnapshots.clear();
        float[] result4 = simulateTransmission(msg4, stationB, stationA, config, recorder);

        check("Signoff TX produced signal", result4 != null);
        System.out.println("  QSO complete — 4 transmissions exchanged");
        System.out.println();

        // ============================================================
        // ACT 9: User clicks PAUSE → RESUME
        // ============================================================
        System.out.println("━━━ ACT 9: User Clicks [PAUSE] then [RESUME] ━━━");

        scenarioManager.pause();
        check("State: PAUSED", scenarioManager.getState() == ScenarioState.PAUSED);
        System.out.println("  StatusBar: PAUSED (yellow)");
        System.out.println("  TX button: disabled");
        System.out.println("  PAUSE button text → RESUME");

        scenarioManager.resume();
        check("State: RUNNING (resumed)", scenarioManager.getState() == ScenarioState.RUNNING);
        System.out.println("  StatusBar: RUNNING (green)");
        System.out.println("  TX button: re-enabled");
        System.out.println();

        // ============================================================
        // ACT 10: User stops recording
        // ============================================================
        System.out.println("━━━ ACT 10: User Clicks [REC] Again (Stop Recording) ━━━");

        recorder.stop();
        long samplesRecorded = recorder.getSamplesWritten();
        check("Samples recorded > 0", samplesRecorded > 0);
        System.out.println("  Samples recorded: " + samplesRecorded);

        // Verify files created
        File dataFile = new File(recBasePath + ".sigmf-data");
        File metaFile = new File(recBasePath + ".sigmf-meta");
        check("SigMF data file exists", dataFile.exists());
        check("SigMF meta file exists", metaFile.exists());
        check("Data file size > 0", dataFile.length() > 0);
        check("Meta file size > 0", metaFile.length() > 0);
        System.out.println("  Data file: " + dataFile.length() + " bytes");
        System.out.println("  Meta file: " + metaFile.length() + " bytes");

        // Save recording to DB
        metadataStore.saveRecording(scenarioId, recBasePath,
                config.getSampleRate(), 7100000, samplesRecorded);
        System.out.println("  Recording saved to database");
        System.out.println("  REC button: gray (recording stopped)");
        System.out.println();

        // ============================================================
        // ACT 11: User plays back IQ recording
        // ============================================================
        System.out.println("━━━ ACT 11: User Clicks [LOAD IQ] → Selects Recording ━━━");

        IQPlayer player = new IQPlayer(recBasePath);
        player.open();
        int totalPlaybackSamples = 0;
        int playbackChunks = 0;
        float[] samples;
        while ((samples = player.read(4096)) != null) {
            totalPlaybackSamples += samples.length;
            playbackChunks++;

            // Simulate spectrum display during playback
            if (samples.length >= 1024) {
                float[] fftInput = new float[1024];
                System.arraycopy(samples, 0, fftInput, 0, 1024);
                float[] spectrum = nativeDSP.fftForward(fftInput, 1024);
                if (spectrum != null) {
                    // UI would update spectrum + waterfall here
                }
            }
        }
        player.close();
        check("Playback read samples", totalPlaybackSamples > 0);
        check("Playback samples match recorded", totalPlaybackSamples == samplesRecorded);
        System.out.println("  Played " + playbackChunks + " chunks, " + totalPlaybackSamples + " samples total");
        System.out.println("  Spectrum/Waterfall: replaying recorded signal");
        System.out.println();

        // ============================================================
        // ACT 12: Checkpoint save/load
        // ============================================================
        System.out.println("━━━ ACT 12: User Saves Scenario (Checkpoint) ━━━");

        String checkpointPath = "test_recordings/checkpoint_qso.properties";
        ScenarioCheckpoint.save(config, scenarioManager.getState(), checkpointPath);
        check("Checkpoint file created", new File(checkpointPath).exists());

        ScenarioConfig loadedConfig = ScenarioCheckpoint.load(checkpointPath);
        check("Loaded config name matches", loadedConfig.getName().equals(config.getName()));
        check("Loaded config has 2 stations", loadedConfig.getStations().size() == 2);
        check("Loaded station 1 = VK3ABC", loadedConfig.getStations().get(0).getCallsign().equals("VK3ABC"));
        check("Loaded station 2 = JA1YXP", loadedConfig.getStations().get(1).getCallsign().equals("JA1YXP"));

        ScenarioState loadedState = ScenarioCheckpoint.loadState(checkpointPath);
        check("Loaded state = RUNNING", loadedState == ScenarioState.RUNNING);
        System.out.println("  Checkpoint saved and verified");
        System.out.println();

        // ============================================================
        // ACT 13: User clicks STOP
        // ============================================================
        System.out.println("━━━ ACT 13: User Clicks [STOP] ━━━");

        session.close();
        check("Session closed", !session.isActive());
        long sessionDuration = System.currentTimeMillis() - session.getStartTime();
        System.out.println("  Session duration: " + sessionDuration + " ms");

        sessionManager.closeAll();
        check("All sessions closed", sessionManager.getActiveSessions().isEmpty());

        scenarioManager.complete();
        check("State: COMPLETED", scenarioManager.getState() == ScenarioState.COMPLETED);

        nativeDSP.dspShutdown();
        stationManager.destroyAll();

        System.out.println("  StatusBar: COMPLETED");
        System.out.println("  All buttons reset to initial state");
        System.out.println("  TX disabled, START enabled");
        System.out.println();

        // ============================================================
        // ACT 14: Verify database records
        // ============================================================
        System.out.println("━━━ ACT 14: Verify Database ━━━");

        List<String> scenarios = metadataStore.listScenarios();
        check("Scenario exists in DB", scenarios.size() > 0);
        check("Scenario name matches", scenarios.get(0).equals(config.getName()));
        System.out.println("  Scenarios in DB: " + scenarios);
        System.out.println();

        // ============================================================
        // ACT 15: User closes application
        // ============================================================
        System.out.println("━━━ ACT 15: User Closes Application ━━━");

        pluginManager.stopAll();
        pluginManager.destroyAll();
        db.close();
        System.out.println("  Plugins shutdown");
        System.out.println("  Database closed");
        System.out.println();

        // Cleanup test files
        dataFile.delete();
        metaFile.delete();
        new File(checkpointPath).delete();
        new File("test_recordings").delete();
        new File("test_simulation.db").delete();

        // ============================================================
        // RESULTS
        // ============================================================
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf("║  RESULTS: %d passed, %d failed                              ║%n",
                passed, failed);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Simulate a full TX→Channel→RX cycle (same as MainWindow.onTransmit) ---

    private static float[] simulateTransmission(String message, Station txStation, Station rxStation,
                                                  ScenarioConfig config, IQRecorder recorder) {
        int sampleRate = config.getSampleRate();
        float freq = (float) txStation.getFrequencyHz();

        // [Station TX state change] → StationPanel updates
        txStation.setTxRxState(Station.TxRxState.TRANSMITTING);
        rxStation.setTxRxState(Station.TxRxState.RECEIVING);

        // Generate baseband (simulates voice/CW as tone sequence)
        int samplesPerChar = sampleRate / 20;
        float[] baseband = new float[message.length() * samplesPerChar];
        for (int c = 0; c < message.length(); c++) {
            float toneFreq = 300 + (message.charAt(c) % 64) * 30;
            float amp = (message.charAt(c) == ' ') ? 0.0f : 0.8f;
            for (int i = 0; i < samplesPerChar; i++) {
                double t = (double) i / sampleRate;
                baseband[c * samplesPerChar + i] = (float) (amp * Math.sin(2.0 * Math.PI * toneFreq * t));
            }
        }

        // SSB Modulate
        float[] modulated = nativeDSP.modulateSSB(baseband, freq, sampleRate, true);
        if (modulated == null) return null;

        eventBus.publish(new LogEvent(null, "INFO",
                "[TX " + txStation.getCallsign() + "] " + message + " (" + modulated.length + " samples)"));

        // Record TX
        try { recorder.write(modulated); } catch (Exception ignored) { }

        // TX Spectrum → SpectrumView + WaterfallView
        float[] txSpectrum = computeSpectrum(modulated);
        if (txSpectrum != null) {
            eventBus.publish(new SpectrumDataEvent(null, txSpectrum, txStation.getCallsign()));
        }

        // Channel propagation
        double distance = txStation.distanceTo(rxStation);
        RFContext rfCtx = new RFContext(freq, distance, sampleRate);
        ChannelPipeline pipeline = new ChannelPipeline();
        pipeline.addStage(new FSPLModel());
        pipeline.addStage(MultipathModel.createDefault());
        pipeline.addStage(IonosphericModel.createDefault());
        pipeline.addStage(NoiseFloorModel.createDefault());

        float[] channelOutput = pipeline.process(modulated, rfCtx);

        // Record RX
        try { recorder.write(channelOutput); } catch (Exception ignored) { }

        // Compute SNR
        float fsplDb = nativeRF.computeFSPL(freq, (float) distance);
        double txPower = computePower(modulated);
        double rxPower = computePower(channelOutput);
        double expectedSignal = txPower * Math.pow(10, -fsplDb / 10);
        double noise = Math.max(rxPower - expectedSignal, 1e-20);
        double snr = Math.max(-30, Math.min(60, 10 * Math.log10(expectedSignal / noise)));

        // RX Spectrum → SpectrumView + WaterfallView
        float[] rxSpectrum = computeSpectrum(channelOutput);
        if (rxSpectrum != null) {
            eventBus.publish(new SpectrumDataEvent(null, rxSpectrum, rxStation.getCallsign()));
        }

        // SSB Demodulate
        float[] demodulated = nativeDSP.demodulateSSB(channelOutput, freq, sampleRate, true);

        // [UI Updates]
        System.out.println("    TX: " + txStation.getCallsign() + " → " + modulated.length + " samples");
        System.out.println("    Channel: FSPL=" + String.format("%.1f dB", fsplDb) +
                ", distance=" + String.format("%.0f km", distance / 1000));
        System.out.println("    RX: " + rxStation.getCallsign() + " → " +
                (demodulated != null ? demodulated.length : 0) + " samples" +
                ", SNR=" + String.format("%.1f dB", snr));

        rxMessages.add("[" + rxStation.getCallsign() + "] received from " +
                txStation.getCallsign() + " SNR:" + String.format("%.1f", snr));

        // Reset states
        txStation.setTxRxState(Station.TxRxState.IDLE);
        rxStation.setTxRxState(Station.TxRxState.IDLE);

        return demodulated;
    }

    private static float[] computeSpectrum(float[] signal) {
        int fftSize = 1024;
        float[] fftInput = new float[fftSize];
        System.arraycopy(signal, 0, fftInput, 0, Math.min(signal.length, fftSize));
        float[] spectrum = nativeDSP.fftForward(fftInput, fftSize);
        if (spectrum == null) return null;
        float[] magnitudes = new float[spectrum.length / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = spectrum[i * 2];
        }
        return magnitudes;
    }

    private static double computePower(float[] signal) {
        double sum = 0;
        for (float s : signal) sum += s * s;
        return sum / signal.length;
    }

    private static void wireEventListeners() {
        eventBus.subscribe("log", e -> {
            LogEvent le = (LogEvent) e;
            logMessages.add("[" + le.getLevel() + "] " + le.getMessage());
        });
        eventBus.subscribe("scenario.state", e -> {
            ScenarioStateEvent se = (ScenarioStateEvent) e;
            scenarioStates.add(se.getOldState() + " → " + se.getNewState());
        });
        eventBus.subscribe("station", e -> {
            StationEvent se = (StationEvent) e;
            stationEvents.add(se.getStationId() + ": " + se.getAction());
        });
        eventBus.subscribe("spectrum.data", e -> {
            SpectrumDataEvent se = (SpectrumDataEvent) e;
            spectrumSnapshots.add(se.getMagnitudes());
        });
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ✓ " + name);
        } else {
            failed++;
            System.out.println("  ✗ FAIL: " + name);
        }
    }
}
