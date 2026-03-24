package com.hamradio.integration;

import com.hamradio.client.*;
import com.hamradio.event.EventBus;
import com.hamradio.event.events.*;
import com.hamradio.protocol.messages.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scripted JavaFX test client — executes a sequence of actions from command-line args.
 *
 * Usage: ScriptedTestClient <callsign> <lat> <lon> <freq> <mode> <actions...>
 *
 * Actions:
 *   tx:<message>          — transmit message
 *   wait:<ms>             — wait N milliseconds
 *   screenshot:<path>     — take screenshot
 *   tune:<freq>:<mode>    — change frequency and mode
 *   exit                  — disconnect and exit
 */
public class ScriptedTestClient extends Application {

    private ServerConnection connection;
    private String callsign;
    private EventBus eventBus;
    private ClientMainWindow mainWindow;
    private Stage stage;

    // Counters for verification
    private final AtomicInteger rxCount = new AtomicInteger(0);
    private final AtomicInteger spectrumCount = new AtomicInteger(0);
    private final AtomicInteger stationUpdateCount = new AtomicInteger(0);
    private final List<String> rxSources = Collections.synchronizedList(new ArrayList<>());
    private final List<String> logMessages = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        List<String> params = getParameters().getRaw();

        callsign = params.get(0);
        double lat = Double.parseDouble(params.get(1));
        double lon = Double.parseDouble(params.get(2));
        double freq = Double.parseDouble(params.get(3));
        String mode = params.get(4);
        List<String> actions = params.subList(5, params.size());

        eventBus = new EventBus();
        MessageDispatcher dispatcher = new MessageDispatcher(eventBus);
        connection = new ServerConnection(dispatcher);

        // Track events
        eventBus.subscribe("signal", e -> rxCount.incrementAndGet());
        eventBus.subscribe("spectrum.data", e -> spectrumCount.incrementAndGet());
        eventBus.subscribe("station", e -> {
            stationUpdateCount.incrementAndGet();
            StationEvent se = (StationEvent) e;
            logMessages.add("[STATION] " + se.getStationId() + ": " + se.getAction());
        });
        eventBus.subscribe("log", e -> {
            LogEvent le = (LogEvent) e;
            logMessages.add("[" + le.getLevel() + "] " + le.getMessage());
        });

        ConnectionDialog.ConnectionInfo info = new ConnectionDialog.ConnectionInfo(
                "localhost", 7100, callsign, lat, lon, freq, mode, true);

        mainWindow = new ClientMainWindow(primaryStage, eventBus, connection, info);

        dispatcher.setOnConnectAck(ack -> Platform.runLater(() -> {
            mainWindow.onConnected(ack);
            print("Connected (SR=" + ack.getSampleRate() + ", stations=" + ack.getStationCallsigns() + ")");

            // Execute actions in background
            new Thread(() -> executeActions(actions, ack.getSampleRate()), callsign + "-script").start();
        }));

        dispatcher.setOnConnectNack(reason -> {
            print("NACK: " + reason);
            Platform.runLater(Platform::exit);
        });

        dispatcher.setOnDisconnect(reason -> {
            print("Disconnected: " + reason);
        });

        mainWindow.build();
        primaryStage.show();

        new Thread(() -> {
            try {
                connection.connect("localhost", 7100);
                connection.sendMessage(new ConnectMessage(
                        callsign, lat, lon, freq, mode, true));
            } catch (Exception e) {
                print("Connect failed: " + e.getMessage());
                Platform.runLater(Platform::exit);
            }
        }, callsign + "-connect").start();
    }

    private void executeActions(List<String> actions, int sampleRate) {
        for (String action : actions) {
            try {
                if (action.startsWith("tx:")) {
                    String msg = action.substring(3);
                    print("TX: " + msg);
                    float[] audio = AudioCapture.generateFromText(msg, sampleRate);
                    connection.sendMessage(new TxBeginMessage(sampleRate));
                    int seq = 0;
                    for (int off = 0; off < audio.length; off += 4096) {
                        int end = Math.min(off + 4096, audio.length);
                        float[] chunk = Arrays.copyOfRange(audio, off, end);
                        connection.sendMessage(new TxAudioMessage(seq++, chunk));
                    }
                    connection.sendMessage(new TxEndMessage(audio.length));
                    print("TX done (" + audio.length + " samples, " + seq + " chunks)");

                } else if (action.startsWith("wait:")) {
                    int ms = Integer.parseInt(action.substring(5));
                    Thread.sleep(ms);

                } else if (action.startsWith("screenshot:")) {
                    String path = action.substring(11);
                    CountDownLatch latch = new CountDownLatch(1);
                    Platform.runLater(() -> {
                        try {
                            WritableImage img = stage.getScene().snapshot(null);
                            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(path));
                            print("Screenshot: " + path + " (" + (int) img.getWidth() + "x" + (int) img.getHeight() + ")");
                        } catch (Exception e) {
                            print("Screenshot failed: " + e.getMessage());
                        }
                        latch.countDown();
                    });
                    latch.await();

                } else if (action.startsWith("tune:")) {
                    String[] parts = action.substring(5).split(":");
                    double newFreq = Double.parseDouble(parts[0]);
                    String newMode = parts.length > 1 ? parts[1] : "SSB";
                    connection.sendMessage(new TuneMessage(newFreq, newMode, true));
                    print("Tuned to " + newFreq + " Hz " + newMode);

                } else if (action.equals("status")) {
                    print("STATUS: rx=" + rxCount.get() + " spectrum=" + spectrumCount.get()
                            + " stationUpdates=" + stationUpdateCount.get()
                            + " logs=" + logMessages.size());

                } else if (action.equals("exit")) {
                    print("Exiting...");
                    try { connection.disconnect(); } catch (Exception ignored) {}
                    Platform.runLater(Platform::exit);
                    return;
                }
            } catch (Exception e) {
                print("Action failed [" + action + "]: " + e.getMessage());
            }
        }
        // Auto-exit after all actions
        print("All actions complete. rx=" + rxCount.get() + " spectrum=" + spectrumCount.get());
        try { connection.disconnect(); } catch (Exception ignored) {}
        Platform.runLater(Platform::exit);
    }

    private void print(String msg) {
        System.out.println("[" + callsign + "] " + msg);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
