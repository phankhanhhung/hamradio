package com.hamradio.ui;

import com.hamradio.control.*;
import com.hamradio.data.*;
import com.hamradio.dsp.NativeDSP;
import com.hamradio.event.EventBus;
import com.hamradio.event.events.*;
import com.hamradio.net.*;
import com.hamradio.plugin.*;
import com.hamradio.rf.*;
import com.hamradio.rf.models.*;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainWindow {

    private final Stage stage;
    private final EventBus eventBus = EventBus.getInstance();
    private final ResourceAllocator resourceAllocator = new ResourceAllocator();
    private final ScenarioManager scenarioManager = new ScenarioManager(eventBus, resourceAllocator);
    private final StationManager stationManager = new StationManager(eventBus);
    private final SessionManager sessionManager = new SessionManager();
    private final NativeDSP nativeDSP = new NativeDSP();

    // Data layer
    private DatabaseManager databaseManager;
    private MetadataStore metadataStore;
    private IQRecorder iqRecorder;
    private long currentScenarioId = -1;
    private boolean recording = false;

    // Plugin framework
    private final PluginRegistry pluginRegistry = new PluginRegistry();
    private final PluginLifecycleManager pluginManager = new PluginLifecycleManager(pluginRegistry);

    // UI components
    private ScenarioConfigPanel configPanel;
    private SpectrumView spectrumView;
    private WaterfallView waterfallView;
    private StationPanel station1Panel;
    private StationPanel station2Panel;
    private TransceiverPanel transceiverPanel;
    private LogPanel logPanel;
    private StatusBar statusBar;
    private UIEventBridge uiEventBridge;

    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private ToggleButton recordButton;
    private Button loadIQButton;
    private SessionManager.Session currentSession;

    public MainWindow(Stage stage) {
        this.stage = stage;
    }

    public void build() {
        // Initialize data layer
        initDatabase();

        // Initialize plugins
        initPlugins();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setPadding(new Insets(5));

        // Left: Scenario config
        configPanel = new ScenarioConfigPanel();
        ScrollPane configScroll = new ScrollPane(configPanel);
        configScroll.setFitToWidth(true);
        configScroll.setPrefWidth(280);
        configScroll.setStyle("-fx-background: #1a1a2e;");

        VBox leftPanel = new VBox(8);
        leftPanel.setPadding(new Insets(5));

        // Control buttons
        startButton = btn("START", "#00cc66", e -> startScenario());
        pauseButton = btn("PAUSE", "#cc9933", e -> pauseScenario());
        stopButton = btn("STOP", "#cc3333", e -> stopScenario());
        pauseButton.setDisable(true);
        stopButton.setDisable(true);

        recordButton = new ToggleButton("REC");
        recordButton.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        recordButton.setPrefWidth(60);
        recordButton.setOnAction(e -> toggleRecording());

        loadIQButton = new Button("LOAD IQ");
        loadIQButton.setStyle("-fx-background-color: #336699; -fx-text-fill: white;");
        loadIQButton.setPrefWidth(80);
        loadIQButton.setOnAction(e -> loadIQFile());

        HBox controlBar = new HBox(5, startButton, pauseButton, stopButton, recordButton, loadIQButton);
        leftPanel.getChildren().addAll(configScroll, controlBar);

        // Center: Spectrum + Waterfall
        spectrumView = new SpectrumView(600, 200);
        waterfallView = new WaterfallView(600, 200);
        VBox centerPanel = new VBox(5, spectrumView, waterfallView);

        // Right: Station panels + transceiver
        station1Panel = new StationPanel();
        station2Panel = new StationPanel();
        transceiverPanel = new TransceiverPanel();
        transceiverPanel.setOnTransmit(this::onTransmit);

        VBox rightPanel = new VBox(8, station1Panel, station2Panel,
                new Separator(Orientation.HORIZONTAL), transceiverPanel);
        rightPanel.setPadding(new Insets(5));
        rightPanel.setPrefWidth(260);

        // Bottom: Log + Status
        logPanel = new LogPanel();
        statusBar = new StatusBar();
        VBox bottomPanel = new VBox(logPanel, statusBar);
        bottomPanel.setPrefHeight(180);

        root.setLeft(leftPanel);
        root.setCenter(centerPanel);
        root.setRight(rightPanel);
        root.setBottom(bottomPanel);

        // Wire event bridge
        uiEventBridge = new UIEventBridge(eventBus);
        uiEventBridge.setOnLog(e -> logPanel.append("[" + e.getLevel() + "] " + e.getMessage()));
        uiEventBridge.setOnScenarioState(e -> {
            statusBar.setState(e.getNewState());
            updateControlButtons(e.getNewState());
        });
        uiEventBridge.setOnSpectrumData(e -> {
            spectrumView.update(e.getMagnitudes());
            waterfallView.addLine(e.getMagnitudes());
        });
        uiEventBridge.setOnStation(e -> updateStationPanels());
        uiEventBridge.bind();

        // Show plugin count
        int pluginCount = pluginRegistry.getAll().size();
        statusBar.setInfo("Plugins: " + pluginCount);

        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("HamRadio - SDR/DSP Simulation Platform (Phase 1)");
        stage.setScene(scene);
    }

    // --- Initialization ---

    private void initDatabase() {
        try {
            databaseManager = new DatabaseManager("hamradio.db");
            databaseManager.initialize();
            metadataStore = new MetadataStore(databaseManager);
        } catch (Exception e) {
            System.err.println("[MainWindow] Database init failed: " + e.getMessage());
        }
    }

    private void initPlugins() {
        try {
            pluginManager.discover();
            pluginManager.initAll();
            pluginManager.startAll();
        } catch (Exception e) {
            System.err.println("[MainWindow] Plugin init: " + e.getMessage());
        }
    }

    // --- Scenario lifecycle ---

    private void startScenario() {
        try {
            ScenarioConfig config = configPanel.buildConfig();
            scenarioManager.reset();
            scenarioManager.setConfig(config);
            scenarioManager.validate();

            stationManager.createFromConfig(config);
            nativeDSP.dspInit(config.getSampleRate());
            scenarioManager.load();

            // Save to database
            if (metadataStore != null) {
                currentScenarioId = metadataStore.saveScenario(
                        config.getName(), config.getPropagationModel(), "RUNNING");
            }

            transceiverPanel.setEnabled(true);
            updateStationPanels();

            int pluginCount = pluginRegistry.getAll().size();
            statusBar.setInfo("SR: " + config.getSampleRate()
                    + " | Stations: " + config.getStations().size()
                    + " | Plugins: " + pluginCount
                    + (recording ? " | REC" : ""));
        } catch (Exception ex) {
            logPanel.append("[ERROR] " + ex.getMessage());
        }
    }

    private void pauseScenario() {
        try {
            if (scenarioManager.getState() == ScenarioState.RUNNING) {
                scenarioManager.pause();
                transceiverPanel.setEnabled(false);
            } else if (scenarioManager.getState() == ScenarioState.PAUSED) {
                scenarioManager.resume();
                transceiverPanel.setEnabled(true);
            }
        } catch (Exception ex) {
            logPanel.append("[ERROR] " + ex.getMessage());
        }
    }

    private void stopScenario() {
        try {
            scenarioManager.complete();
            nativeDSP.dspShutdown();
            transceiverPanel.setEnabled(false);

            // Close session
            if (currentSession != null) {
                currentSession.close();
                long duration = System.currentTimeMillis() - currentSession.getStartTime();
                eventBus.publish(new LogEvent(this, "INFO",
                        "[Session] QSO ended, duration: " + (duration / 1000) + "s"));
                currentSession = null;
            }

            // Stop recording
            if (recording) {
                stopRecording();
            }

            sessionManager.closeAll();
            stationManager.destroyAll();
        } catch (Exception ex) {
            logPanel.append("[ERROR] " + ex.getMessage());
        }
    }

    // --- IQ Recording ---

    private void toggleRecording() {
        if (recordButton.isSelected()) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        if (scenarioManager.getState() != ScenarioState.RUNNING) {
            recording = true;
            recordButton.setStyle("-fx-background-color: #cc3333; -fx-text-fill: white; -fx-font-weight: bold;");
            logPanel.append("[REC] Recording armed — will start on next TX");
            return;
        }
        try {
            ScenarioConfig config = scenarioManager.getConfig();
            String basePath = "recordings/rec_" + System.currentTimeMillis();
            new File("recordings").mkdirs();
            iqRecorder = new IQRecorder(basePath, config.getSampleRate(),
                    config.getStations().get(0).getFrequencyHz());
            iqRecorder.start();
            recording = true;
            recordButton.setStyle("-fx-background-color: #cc3333; -fx-text-fill: white; -fx-font-weight: bold;");
            logPanel.append("[REC] Recording started: " + basePath);
        } catch (Exception e) {
            logPanel.append("[ERROR] Recording failed: " + e.getMessage());
            recording = false;
            recordButton.setSelected(false);
        }
    }

    private void stopRecording() {
        if (iqRecorder != null) {
            try {
                iqRecorder.stop();
                long samples = iqRecorder.getSamplesWritten();
                logPanel.append("[REC] Recording stopped: " + samples + " samples saved");

                // Save to database
                ScenarioConfig config = scenarioManager.getConfig();
                if (metadataStore != null && currentScenarioId > 0 && config != null) {
                    metadataStore.saveRecording(currentScenarioId, "recordings/",
                            config.getSampleRate(),
                            config.getStations().get(0).getFrequencyHz(), samples);
                }
                iqRecorder = null;
            } catch (Exception e) {
                logPanel.append("[ERROR] Stop recording: " + e.getMessage());
            }
        }
        recording = false;
        recordButton.setSelected(false);
        recordButton.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
    }

    // --- IQ Playback ---

    private void loadIQFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load IQ Recording");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SigMF Data", "*.sigmf-data"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        String basePath = file.getAbsolutePath().replace(".sigmf-data", "");
        // Ensure DSP is initialized for FFT
        nativeDSP.dspInit(44100);
        new Thread(() -> {
            try {
                IQPlayer player = new IQPlayer(basePath);
                player.open();
                float[] samples;
                int totalSamples = 0;
                while ((samples = player.read(4096)) != null) {
                    totalSamples += samples.length;
                    // Show spectrum
                    int fftSize = 1024;
                    if (samples.length >= fftSize) {
                        float[] fftInput = new float[fftSize];
                        System.arraycopy(samples, 0, fftInput, 0, fftSize);
                        float[] spectrum = nativeDSP.fftForward(fftInput, fftSize);
                        if (spectrum != null) {
                            float[] magnitudes = extractMagnitudes(spectrum);
                            eventBus.publish(new SpectrumDataEvent(this, magnitudes, "playback"));
                        }
                    }
                    Thread.sleep(50); // pace playback
                }
                player.close();
                int total = totalSamples;
                eventBus.publish(new LogEvent(this, "INFO",
                        "[Playback] Complete: " + total + " samples from " + basePath));
            } catch (Exception e) {
                eventBus.publish(new LogEvent(this, "ERROR", "Playback error: " + e.getMessage()));
            }
        }, "iq-playback").start();
    }

    // --- Transmit ---

    private void onTransmit(String message) {
        if (scenarioManager.getState() != ScenarioState.RUNNING) return;

        new Thread(() -> {
            try {
                ScenarioConfig config = scenarioManager.getConfig();
                int sampleRate = config.getSampleRate();
                String mode = config.getStations().get(0).getMode();
                float freq = (float) config.getStations().get(0).getFrequencyHz();

                List<Station> stations = new ArrayList<>(stationManager.getAllStations());
                if (stations.size() < 2) return;

                Station txStation = stations.get(0);
                Station rxStation = stations.get(1);

                // Create session on first TX
                if (currentSession == null) {
                    currentSession = sessionManager.createSession(
                            txStation.getCallsign(), rxStation.getCallsign());
                    eventBus.publish(new LogEvent(this, "INFO",
                            "[Session] QSO started: " + txStation.getCallsign() + " ↔ " + rxStation.getCallsign()));
                }

                // Update station states
                Platform.runLater(() -> {
                    station1Panel.setTxRxState("TRANSMITTING");
                    station2Panel.setTxRxState("RECEIVING");
                });

                // Generate baseband
                float[] baseband = generateMessageTone(message, sampleRate);

                // Modulate
                float[] modulated = modulate(baseband, mode, freq, sampleRate);
                if (modulated == null) {
                    eventBus.publish(new LogEvent(this, "ERROR", "Modulation failed"));
                    return;
                }

                eventBus.publish(new LogEvent(this, "INFO",
                        "[TX " + txStation.getCallsign() + "] " + message + " (" + modulated.length + " samples)"));

                // Record TX signal
                if (recording && iqRecorder != null) {
                    iqRecorder.write(modulated);
                } else if (recording && iqRecorder == null) {
                    startRecordingForScenario();
                    if (iqRecorder != null) iqRecorder.write(modulated);
                }

                // TX spectrum
                float[] txSpectrum = computeSpectrum(modulated);
                if (txSpectrum != null) {
                    eventBus.publish(new SpectrumDataEvent(this, txSpectrum, txStation.getCallsign()));
                }

                // Apply channel model
                double distance = txStation.distanceTo(rxStation);
                RFContext rfCtx = new RFContext(freq, distance, sampleRate);
                ChannelPipeline pipeline = buildPipeline(config.getPropagationModel());
                float[] channelOutput = pipeline.process(modulated, rfCtx);

                // Compute SNR: compare received signal power to expected noise floor
                double rxPower = computePower(channelOutput);
                double fsplLoss = computeFSPLdB(freq, distance);
                double expectedSignalPower = computePower(modulated) * Math.pow(10, -fsplLoss / 10);
                double noisePower = Math.max(rxPower - expectedSignalPower, 1e-20);
                final double estimatedSnr = Math.max(-30, Math.min(60,
                        10 * Math.log10(expectedSignalPower / noisePower)));

                // Record channel output
                if (recording && iqRecorder != null) {
                    iqRecorder.write(channelOutput);
                }

                // RX spectrum
                float[] rxSpectrum = computeSpectrum(channelOutput);

                // Demodulate
                float[] demodulated = demodulate(channelOutput, mode, freq, sampleRate);

                Platform.runLater(() -> {
                    // Show RX spectrum (after TX spectrum has been shown)
                    if (rxSpectrum != null) {
                        spectrumView.update(rxSpectrum);
                        waterfallView.addLine(rxSpectrum);
                    }

                    // Update SNR on RX station
                    station2Panel.setSNR(estimatedSnr);

                    // Show RX result
                    if (demodulated != null) {
                        transceiverPanel.appendRx("[" + rxStation.getCallsign() + "] Signal received ("
                                + demodulated.length + " samples, "
                                + String.format("%.0f km", distance / 1000) + ", "
                                + String.format("SNR: %.1f dB", estimatedSnr) + ")");
                    }

                    // Reset states
                    station1Panel.setTxRxState("IDLE");
                    station2Panel.setTxRxState("IDLE");

                    NativeRF nativeRF = new NativeRF();
                    float fsplDb = nativeRF.computeFSPL(freq, (float) distance);
                    logPanel.append("[RX " + rxStation.getCallsign() + "] "
                            + demodulated.length + " samples, FSPL: "
                            + String.format("%.1f dB", fsplDb) + ", SNR: "
                            + String.format("%.1f dB", estimatedSnr));
                });

            } catch (Exception ex) {
                eventBus.publish(new LogEvent(this, "ERROR", "TX error: " + ex.getMessage()));
                Platform.runLater(() -> {
                    station1Panel.setTxRxState("IDLE");
                    station2Panel.setTxRxState("IDLE");
                });
            }
        }, "tx-thread").start();
    }

    private void startRecordingForScenario() {
        try {
            ScenarioConfig config = scenarioManager.getConfig();
            String basePath = "recordings/rec_" + System.currentTimeMillis();
            new File("recordings").mkdirs();
            iqRecorder = new IQRecorder(basePath, config.getSampleRate(),
                    config.getStations().get(0).getFrequencyHz());
            iqRecorder.start();
            Platform.runLater(() -> logPanel.append("[REC] Recording started: " + basePath));
        } catch (Exception e) {
            Platform.runLater(() -> logPanel.append("[ERROR] Recording: " + e.getMessage()));
        }
    }

    // --- DSP helpers ---

    private float[] modulate(float[] baseband, String mode, float freq, int sampleRate) {
        switch (mode) {
            case "AM":  return nativeDSP.modulateAM(baseband, freq, sampleRate);
            case "FM":  return nativeDSP.modulateFM(baseband, freq, 5000f, sampleRate);
            case "SSB":
            default:    return nativeDSP.modulateSSB(baseband, freq, sampleRate, true);
        }
    }

    private float[] demodulate(float[] signal, String mode, float freq, int sampleRate) {
        switch (mode) {
            case "AM":  return nativeDSP.demodulateAM(signal, freq, sampleRate);
            case "FM":  return nativeDSP.demodulateFM(signal, freq, 5000f, sampleRate);
            case "SSB":
            default:    return nativeDSP.demodulateSSB(signal, freq, sampleRate, true);
        }
    }

    private float[] computeSpectrum(float[] signal) {
        int fftSize = 1024;
        float[] fftInput = new float[fftSize];
        System.arraycopy(signal, 0, fftInput, 0, Math.min(signal.length, fftSize));
        float[] spectrum = nativeDSP.fftForward(fftInput, fftSize);
        return spectrum != null ? extractMagnitudes(spectrum) : null;
    }

    private float[] extractMagnitudes(float[] spectrum) {
        float[] magnitudes = new float[spectrum.length / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = spectrum[i * 2]; // magnitude at even indices
        }
        return magnitudes;
    }

    private double computePower(float[] signal) {
        double sum = 0;
        for (float s : signal) sum += s * s;
        return sum / signal.length;
    }

    private float peakAbs(float[] signal) {
        float peak = 0;
        for (float s : signal) {
            float a = Math.abs(s);
            if (a > peak) peak = a;
        }
        return peak;
    }

    private double computeFSPLdB(float freq, double distance) {
        return 20 * Math.log10(distance) + 20 * Math.log10(freq) - 147.55;
    }

    private float[] generateMessageTone(String message, int sampleRate) {
        int samplesPerChar = sampleRate / 20;
        float[] tone = new float[message.length() * samplesPerChar];
        for (int c = 0; c < message.length(); c++) {
            float freq = 300 + (message.charAt(c) % 64) * 30;
            float amp = (message.charAt(c) == ' ') ? 0.0f : 0.8f;
            for (int i = 0; i < samplesPerChar; i++) {
                double t = (double) i / sampleRate;
                tone[c * samplesPerChar + i] = (float) (amp * Math.sin(2.0 * Math.PI * freq * t));
            }
        }
        return tone;
    }

    private ChannelPipeline buildPipeline(String model) {
        ChannelPipeline pipeline = new ChannelPipeline();
        switch (model) {
            case "fspl":
                pipeline.addStage(new FSPLModel());
                break;
            case "multipath":
                pipeline.addStage(new FSPLModel());
                pipeline.addStage(MultipathModel.createDefault());
                break;
            case "ionospheric":
                pipeline.addStage(new FSPLModel());
                pipeline.addStage(IonosphericModel.createDefault());
                break;
            case "full":
            default:
                pipeline.addStage(new FSPLModel());
                pipeline.addStage(MultipathModel.createDefault());
                pipeline.addStage(IonosphericModel.createDefault());
                pipeline.addStage(NoiseFloorModel.createDefault());
                break;
        }
        // Add propagation models from plugins
        for (PropagationModelPlugin p : pluginRegistry.getPluginsByType(PropagationModelPlugin.class)) {
            for (PropagationModel m : p.getModels()) {
                pipeline.addStage(m);
            }
        }
        return pipeline;
    }

    // --- UI helpers ---

    private void updateStationPanels() {
        List<Station> stations = new ArrayList<>(stationManager.getAllStations());
        if (stations.size() > 0) station1Panel.update(stations.get(0));
        if (stations.size() > 1) station2Panel.update(stations.get(1));
    }

    private void updateControlButtons(String state) {
        switch (state) {
            case "RUNNING":
                startButton.setDisable(true);
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
                pauseButton.setText("PAUSE");
                break;
            case "PAUSED":
                startButton.setDisable(true);
                pauseButton.setDisable(false);
                stopButton.setDisable(false);
                pauseButton.setText("RESUME");
                break;
            case "COMPLETED":
            case "FAILED":
                startButton.setDisable(false);
                pauseButton.setDisable(true);
                stopButton.setDisable(true);
                transceiverPanel.setEnabled(false);
                break;
            default:
                startButton.setDisable(false);
                pauseButton.setDisable(true);
                stopButton.setDisable(true);
                break;
        }
    }

    private Button btn(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold;");
        b.setPrefWidth(80);
        b.setOnAction(handler);
        return b;
    }

    public void shutdown() {
        pluginManager.stopAll();
        pluginManager.destroyAll();
        if (databaseManager != null) databaseManager.close();
    }
}
