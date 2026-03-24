package com.hamradio.integration;

import com.hamradio.client.*;
import com.hamradio.event.EventBus;
import com.hamradio.protocol.messages.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-test JavaFX client — connects, optionally transmits, takes screenshot.
 * Usage: AutoTestClient <callsign> <lat> <lon> <screenshotPath> [txMessage]
 */
public class AutoTestClient extends Application {

    @Override
    public void start(Stage stage) {
        List<String> params = getParameters().getRaw();
        String callsign = params.get(0);
        double lat = Double.parseDouble(params.get(1));
        double lon = Double.parseDouble(params.get(2));
        String screenshotPath = params.get(3);
        String txMessage = params.size() > 4 ? params.get(4) : null;

        EventBus eventBus = new EventBus();
        MessageDispatcher dispatcher = new MessageDispatcher(eventBus);
        ServerConnection connection = new ServerConnection(dispatcher);

        ConnectionDialog.ConnectionInfo info = new ConnectionDialog.ConnectionInfo(
                "localhost", 7100, callsign, lat, lon, 7100000, "SSB", true
        );

        ClientMainWindow mainWindow = new ClientMainWindow(stage, eventBus, connection, info);

        dispatcher.setOnConnectAck(ack -> Platform.runLater(() -> {
            mainWindow.onConnected(ack);
            System.out.println("[" + callsign + "] Connected! SR=" + ack.getSampleRate());

            if (txMessage != null) {
                // Auto-transmit after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        System.out.println("[" + callsign + "] TX: " + txMessage);
                        float[] audio = AudioCapture.generateFromText(txMessage, ack.getSampleRate());
                        connection.sendMessage(new TxBeginMessage(ack.getSampleRate()));
                        int seq = 0;
                        for (int off = 0; off < audio.length; off += 4096) {
                            int end = Math.min(off + 4096, audio.length);
                            connection.sendMessage(new TxAudioMessage(seq++, Arrays.copyOfRange(audio, off, end)));
                        }
                        connection.sendMessage(new TxEndMessage(audio.length));
                        System.out.println("[" + callsign + "] TX complete, " + audio.length + " samples");

                        Thread.sleep(3000);
                        Platform.runLater(() -> screenshot(stage, screenshotPath, callsign));
                    } catch (Exception e) { e.printStackTrace(); }
                }, callsign + "-tx").start();
            } else {
                // Receiver — wait for incoming, then screenshot
                new Thread(() -> {
                    try {
                        Thread.sleep(8000);
                        Platform.runLater(() -> screenshot(stage, screenshotPath, callsign));
                    } catch (Exception e) { e.printStackTrace(); }
                }, callsign + "-wait").start();
            }
        }));

        dispatcher.setOnConnectNack(reason -> {
            System.err.println("[" + callsign + "] NACK: " + reason);
            Platform.exit();
        });

        mainWindow.build();
        stage.show();

        new Thread(() -> {
            try {
                connection.connect("localhost", 7100);
                connection.sendMessage(new ConnectMessage(
                        callsign, lat, lon, 7100000, "SSB", true));
            } catch (Exception e) {
                System.err.println("[" + callsign + "] Connect failed: " + e.getMessage());
                Platform.runLater(Platform::exit);
            }
        }, callsign + "-connect").start();
    }

    private void screenshot(Stage stage, String path, String callsign) {
        try {
            WritableImage img = stage.getScene().snapshot(null);
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(path));
            System.out.println("[" + callsign + "] Screenshot: " + path +
                    " (" + (int) img.getWidth() + "x" + (int) img.getHeight() + ")");
        } catch (Exception e) {
            System.err.println("[" + callsign + "] Screenshot failed: " + e.getMessage());
        }
        // Exit after screenshot
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
