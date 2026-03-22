package com.hamradio.client;

import com.hamradio.event.EventBus;
import com.hamradio.event.events.*;
import com.hamradio.net.Station;
import com.hamradio.protocol.messages.*;
import com.hamradio.ui.*;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class ClientMainWindow {

    private final Stage stage;
    private final EventBus eventBus;
    private final ServerConnection connection;
    private final ConnectionDialog.ConnectionInfo info;
    private int sampleRate = 44100;

    // UI panels
    private SpectrumView spectrumView;
    private WaterfallView waterfallView;
    private StationPanel localStationPanel;
    private StationPanel remoteStationPanel;
    private TransceiverPanel transceiverPanel;
    private LogPanel logPanel;
    private StatusBar statusBar;
    private UIEventBridge uiEventBridge;

    // Voice I/O
    private MicrophoneInput micInput;
    private SpeakerOutput speakerOutput;
    private volatile boolean pttActive = false;
    private Thread voiceTxThread;
    private AnimationTimer levelMeterTimer;

    private final Map<String, Station> remoteStations = new HashMap<>();

    public ClientMainWindow(Stage stage, EventBus eventBus,
                            ServerConnection connection, ConnectionDialog.ConnectionInfo info) {
        this.stage = stage;
        this.eventBus = eventBus;
        this.connection = connection;
        this.info = info;
    }

    public void build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setPadding(new Insets(5));

        // Left: Local station panel
        localStationPanel = new StationPanel();
        VBox leftPanel = new VBox(8);
        leftPanel.setPadding(new Insets(5));
        leftPanel.setPrefWidth(260);
        Label localTitle = new Label("LOCAL STATION");
        localTitle.setStyle("-fx-text-fill: #ff6600; -fx-font-family: 'Monospaced'; -fx-font-size: 14;");
        leftPanel.getChildren().addAll(localTitle, localStationPanel);

        // Center: Spectrum + Waterfall
        spectrumView = new SpectrumView(600, 200);
        waterfallView = new WaterfallView(600, 200);
        VBox centerPanel = new VBox(5, spectrumView, waterfallView);

        // Right: Remote station + Transceiver
        remoteStationPanel = new StationPanel();
        transceiverPanel = new TransceiverPanel();
        transceiverPanel.setEnabled(false);
        transceiverPanel.setOnTransmit(this::onTransmitText);
        transceiverPanel.setOnPttPress(this::onPttPress);
        transceiverPanel.setOnPttRelease(this::onPttRelease);

        VBox rightPanel = new VBox(8);
        rightPanel.setPadding(new Insets(5));
        rightPanel.setPrefWidth(280);
        Label remoteTitle = new Label("REMOTE STATIONS");
        remoteTitle.setStyle("-fx-text-fill: #ff6600; -fx-font-family: 'Monospaced'; -fx-font-size: 14;");
        rightPanel.getChildren().addAll(remoteTitle, remoteStationPanel,
                new Separator(Orientation.HORIZONTAL), transceiverPanel);

        // Bottom: Log + Status bar
        logPanel = new LogPanel();
        statusBar = new StatusBar();
        VBox bottomPanel = new VBox(logPanel, statusBar);
        bottomPanel.setPrefHeight(180);

        root.setLeft(leftPanel);
        root.setCenter(centerPanel);
        root.setRight(rightPanel);
        root.setBottom(bottomPanel);

        // Wire UIEventBridge
        uiEventBridge = new UIEventBridge(eventBus);
        uiEventBridge.setOnLog(e -> logPanel.append("[" + e.getLevel() + "] " + e.getMessage()));
        uiEventBridge.setOnScenarioState(e -> {
            statusBar.setState(e.getNewState());
            logPanel.append("[SCENARIO] " + e.getOldState() + " -> " + e.getNewState());
        });
        uiEventBridge.setOnSpectrumData(e -> {
            spectrumView.update(e.getMagnitudes());
            waterfallView.addLine(e.getMagnitudes());
        });
        uiEventBridge.setOnStation(e ->
                logPanel.append("[STATION] " + e.getStationId() + ": " + e.getAction()));
        uiEventBridge.setOnSignal(e -> {
            float[] samples = e.getSamples();
            // Play received audio through speaker
            if (speakerOutput != null && speakerOutput.isRunning()) {
                speakerOutput.play(samples);
            }
            transceiverPanel.appendRx("[RX] Audio (" + samples.length + " samples)");
        });
        uiEventBridge.bind();

        // Update local station panel
        Station localStation = new Station(
                info.callsign, info.latitude, info.longitude,
                info.frequencyHz, info.mode);
        localStation.setUpperSideband(info.upperSideband);
        localStationPanel.update(localStation);
        localStationPanel.setTxRxState("IDLE");

        statusBar.setState("CONNECTING");
        statusBar.setInfo("Server: " + info.host + ":" + info.port);

        logPanel.append("[CLIENT] Connecting to " + info.host + ":" + info.port + "...");
        logPanel.append("[CLIENT] Local station: " + info.callsign
                + " @ " + String.format("%.0f Hz", info.frequencyHz) + " " + info.mode);

        // Initialize speaker output
        initSpeaker();

        // Start level meter animation
        startLevelMeters();

        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("HamRadio Client - " + info.callsign);
        stage.setScene(scene);
        stage.show();
    }

    // --- Audio I/O initialization ---

    private void initSpeaker() {
        try {
            speakerOutput = new SpeakerOutput(sampleRate);
            speakerOutput.start();
            logPanel.append("[AUDIO] Speaker output initialized");
        } catch (Exception e) {
            logPanel.append("[AUDIO] Speaker unavailable: " + e.getMessage());
            speakerOutput = null;
        }
    }

    private void initMicrophone() {
        try {
            micInput = new MicrophoneInput(sampleRate, 4096);
            micInput.start();
            logPanel.append("[AUDIO] Microphone input initialized");
        } catch (Exception e) {
            logPanel.append("[AUDIO] Microphone unavailable: " + e.getMessage());
            micInput = null;
        }
    }

    private void startLevelMeters() {
        levelMeterTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (micInput != null && micInput.isRunning()) {
                    transceiverPanel.setTxLevel(micInput.getPeakLevel());
                }
                if (speakerOutput != null && speakerOutput.isRunning()) {
                    transceiverPanel.setRxLevel(speakerOutput.getPeakLevel());
                }
            }
        };
        levelMeterTimer.start();
    }

    // --- Connection callback ---

    public void onConnected(ConnectAckMessage ack) {
        Platform.runLater(() -> {
            sampleRate = ack.getSampleRate();

            // Re-init speaker with correct sample rate
            if (speakerOutput != null) speakerOutput.stop();
            initSpeaker();

            statusBar.setState("RUNNING");
            statusBar.setInfo("Session: " + ack.getSessionId()
                    + " | SR: " + sampleRate
                    + " | Model: " + ack.getPropagationModel()
                    + " | Stations: " + ack.getStationCallsigns().size());

            logPanel.append("[CLIENT] Connected! Session: " + ack.getSessionId());
            logPanel.append("[CLIENT] Sample rate: " + sampleRate
                    + " Hz, Propagation: " + ack.getPropagationModel());

            List<String> callsigns = ack.getStationCallsigns();
            for (String callsign : callsigns) {
                if (!callsign.equals(info.callsign)) {
                    Station remote = new Station(callsign, 0, 0, info.frequencyHz, info.mode);
                    remoteStations.put(callsign, remote);
                    remoteStationPanel.update(remote);
                    remoteStationPanel.setTxRxState("IDLE");
                    logPanel.append("[CLIENT] Remote station: " + callsign);
                }
            }
            if (callsigns.isEmpty()) {
                logPanel.append("[CLIENT] No other stations connected yet");
            }

            transceiverPanel.setEnabled(true);
        });
    }

    // --- Text TX (existing) ---

    private void onTransmitText(String message) {
        if (!connection.isConnected()) return;

        transceiverPanel.setEnabled(false);
        localStationPanel.setTxRxState("TRANSMITTING");

        new Thread(() -> {
            try {
                float[] audio = AudioCapture.generateFromText(message, sampleRate);
                Platform.runLater(() -> logPanel.append(
                        "[TX " + info.callsign + "] " + message + " (" + audio.length + " samples)"));

                sendAudioToServer(audio);

                Platform.runLater(() -> {
                    localStationPanel.setTxRxState("IDLE");
                    transceiverPanel.setEnabled(true);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    localStationPanel.setTxRxState("IDLE");
                    transceiverPanel.setEnabled(true);
                    logPanel.append("[ERROR] TX failed: " + e.getMessage());
                });
            }
        }, "client-text-tx").start();
    }

    // --- Voice PTT (new) ---

    private void onPttPress() {
        if (!connection.isConnected()) return;
        if (pttActive) return;

        // Init mic on first PTT press (lazy init to avoid blocking startup)
        if (micInput == null) {
            initMicrophone();
            if (micInput == null) {
                Platform.runLater(() -> logPanel.append("[ERROR] No microphone available"));
                return;
            }
        }

        pttActive = true;
        localStationPanel.setTxRxState("TRANSMITTING");

        voiceTxThread = new Thread(() -> {
            try {
                // Send TX_BEGIN
                connection.sendMessage(new TxBeginMessage(sampleRate));
                Platform.runLater(() -> logPanel.append("[TX " + info.callsign + "] Voice PTT active"));

                int sequence = 0;
                long totalSamples = 0;

                // Stream mic audio to server while PTT is held
                while (pttActive && connection.isConnected()) {
                    float[] chunk = micInput.poll();
                    if (chunk != null) {
                        connection.sendMessage(new TxAudioMessage(sequence++, chunk));
                        totalSamples += chunk.length;
                    } else {
                        Thread.sleep(5); // avoid busy-wait
                    }
                }

                // Drain remaining mic buffer
                float[] remaining;
                while ((remaining = micInput.poll()) != null) {
                    connection.sendMessage(new TxAudioMessage(sequence++, remaining));
                    totalSamples += remaining.length;
                }

                // Send TX_END
                connection.sendMessage(new TxEndMessage(totalSamples));

                final long total = totalSamples;
                final int chunks = sequence;
                Platform.runLater(() -> {
                    localStationPanel.setTxRxState("IDLE");
                    logPanel.append("[TX " + info.callsign + "] Voice TX complete ("
                            + total + " samples, " + chunks + " chunks, "
                            + String.format("%.1f", (double) total / sampleRate) + "s)");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    localStationPanel.setTxRxState("IDLE");
                    logPanel.append("[ERROR] Voice TX: " + e.getMessage());
                });
            }
        }, "client-voice-tx");
        voiceTxThread.setDaemon(true);
        voiceTxThread.start();
    }

    private void onPttRelease() {
        pttActive = false;
        // voiceTxThread will drain and send TX_END
    }

    // --- Shared TX helper ---

    private void sendAudioToServer(float[] audio) throws IOException {
        connection.sendMessage(new TxBeginMessage(sampleRate));
        int chunkSize = 4096;
        int sequence = 0;
        for (int offset = 0; offset < audio.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, audio.length);
            float[] chunk = Arrays.copyOfRange(audio, offset, end);
            connection.sendMessage(new TxAudioMessage(sequence++, chunk));
        }
        connection.sendMessage(new TxEndMessage(audio.length));
    }

    // --- Disconnect ---

    public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            statusBar.setState("FAILED");
            statusBar.setInfo("Disconnected");
            transceiverPanel.setEnabled(false);
            localStationPanel.setTxRxState("IDLE");
            logPanel.append("[CLIENT] Disconnected: " + reason);
        });
    }

    public void shutdown() {
        pttActive = false;
        if (levelMeterTimer != null) levelMeterTimer.stop();
        if (micInput != null) micInput.stop();
        if (speakerOutput != null) speakerOutput.stop();
    }
}
