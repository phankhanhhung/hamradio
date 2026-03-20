package com.hamradio.client;

import com.hamradio.event.EventBus;
import com.hamradio.protocol.messages.ConnectMessage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * JavaFX Application entry point for the HamRadio network client.
 * Shows a connection dialog, connects to the server, and launches
 * the main client window.
 */
public class HamRadioClient extends Application {

    private ServerConnection connection;
    private ClientMainWindow mainWindow;

    @Override
    public void start(Stage primaryStage) {
        // 1. Show ConnectionDialog
        ConnectionDialog dialog = new ConnectionDialog();
        Optional<ConnectionDialog.ConnectionInfo> result = dialog.showAndWait();

        // 2. If cancelled, exit
        if (!result.isPresent() || result.get() == null) {
            Platform.exit();
            return;
        }

        ConnectionDialog.ConnectionInfo info = result.get();

        // 3. Create EventBus, MessageDispatcher, ServerConnection
        EventBus eventBus = new EventBus();
        MessageDispatcher dispatcher = new MessageDispatcher(eventBus);
        connection = new ServerConnection(dispatcher);

        // 4. Build ClientMainWindow (shown but not yet "connected" state)
        mainWindow = new ClientMainWindow(primaryStage, eventBus, connection, info);
        mainWindow.build();

        // 5. Set dispatcher callbacks
        dispatcher.setOnConnectAck(ack -> mainWindow.onConnected(ack));

        dispatcher.setOnConnectNack(reason -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Rejected");
            alert.setHeaderText("Server rejected connection");
            alert.setContentText(reason);
            alert.showAndWait();
            Platform.exit();
        }));

        dispatcher.setOnDisconnect(reason -> mainWindow.onDisconnected(reason));

        // 6. Connect to server on a background thread
        new Thread(() -> {
            try {
                connection.connect(info.host, info.port);

                // 7. Send CONNECT message
                ConnectMessage connectMsg = new ConnectMessage(
                        info.callsign, info.latitude, info.longitude,
                        info.frequencyHz, info.mode, info.upperSideband);
                connection.sendMessage(connectMsg);

                // 8. On CONNECT_ACK, the dispatcher callback calls mainWindow.onConnected()
                // This is handled asynchronously by the reader thread

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Failed");
                    alert.setHeaderText("Could not connect to server");
                    alert.setContentText(info.host + ":" + info.port + "\n" + e.getMessage());
                    alert.showAndWait();
                    Platform.exit();
                });
            }
        }, "client-connect-thread").start();

        // 9. On close, disconnect gracefully
        primaryStage.setOnCloseRequest(event -> {
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
            }
        });
    }

    @Override

}
