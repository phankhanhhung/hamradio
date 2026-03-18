package com.hamradio.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

public class TransceiverPanel extends VBox {

    private final TextField txField = new TextField("CQ CQ CQ DE VK3ABC");
    private final Button transmitButton = new Button("TX");
    private final Button pttButton = new Button("PTT (Hold)");
    private final TextArea rxArea = new TextArea();
    private final ProgressBar txLevel = new ProgressBar(0);
    private final ProgressBar rxLevel = new ProgressBar(0);
    private final Label txLevelLabel = new Label("TX: --");
    private final Label rxLevelLabel = new Label("RX: --");
    private final Label modeLabel = new Label("Mode: Text");

    private Consumer<String> onTransmit;     // text TX
    private Runnable onPttPress;             // voice PTT press
    private Runnable onPttRelease;           // voice PTT release
    private boolean voiceMode = false;

    public TransceiverPanel() {
        setSpacing(8);
        setPadding(new Insets(10));

        Label title = new Label("TRANSCEIVER");
        title.setFont(Font.font("Monospaced", 14));
        title.setTextFill(Color.web("#ff6600"));

        // Mode toggle
        ToggleButton voiceToggle = new ToggleButton("VOICE");
        voiceToggle.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        voiceToggle.setOnAction(e -> {
            voiceMode = voiceToggle.isSelected();
            if (voiceMode) {
                voiceToggle.setStyle("-fx-background-color: #cc3333; -fx-text-fill: white; -fx-font-weight: bold;");
                modeLabel.setText("Mode: Voice (Mic)");
                txField.setDisable(true);
                transmitButton.setDisable(true);
                pttButton.setDisable(false);
            } else {
                voiceToggle.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
                modeLabel.setText("Mode: Text");
                txField.setDisable(false);
                transmitButton.setDisable(false);
                pttButton.setDisable(true);
            }
        });

        modeLabel.setTextFill(Color.LIGHTGRAY);
        modeLabel.setFont(Font.font("Monospaced", 10));

        HBox titleBar = new HBox(10, title, voiceToggle, modeLabel);

        // Text TX
        txField.setPromptText("Message to transmit");
        HBox.setHgrow(txField, Priority.ALWAYS);
        transmitButton.setStyle("-fx-background-color: #cc3333; -fx-text-fill: white; -fx-font-weight: bold;");
        transmitButton.setDisable(true);
        transmitButton.setOnAction(e -> {
            if (onTransmit != null && !txField.getText().isEmpty()) {
                onTransmit.accept(txField.getText());
            }
        });
        HBox txRow = new HBox(8, txField, transmitButton);

        // PTT button (voice mode)
        pttButton.setMaxWidth(Double.MAX_VALUE);
        pttButton.setStyle("-fx-background-color: #336699; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        pttButton.setPrefHeight(40);
        pttButton.setDisable(true);
        pttButton.setOnMousePressed(e -> {
            if (onPttPress != null) {
                onPttPress.run();
                pttButton.setStyle("-fx-background-color: #cc3333; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
                pttButton.setText(">>> TRANSMITTING <<<");
            }
        });
        pttButton.setOnMouseReleased(e -> {
            if (onPttRelease != null) {
                onPttRelease.run();
                pttButton.setStyle("-fx-background-color: #336699; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
                pttButton.setText("PTT (Hold)");
            }
        });

        // Level meters
        txLevel.setPrefWidth(200);
        txLevel.setPrefHeight(8);
        txLevel.setStyle("-fx-accent: #cc3333;");
        txLevelLabel.setTextFill(Color.web("#cc6666"));
        txLevelLabel.setFont(Font.font("Monospaced", 9));

        rxLevel.setPrefWidth(200);
        rxLevel.setPrefHeight(8);
        rxLevel.setStyle("-fx-accent: #00cc66;");
        rxLevelLabel.setTextFill(Color.web("#66cc66"));
        rxLevelLabel.setFont(Font.font("Monospaced", 9));

        HBox txMeter = new HBox(5, txLevelLabel, txLevel);
        HBox rxMeter = new HBox(5, rxLevelLabel, rxLevel);

        // RX display
        rxArea.setEditable(false);
        rxArea.setPrefRowCount(4);
        rxArea.setStyle("-fx-control-inner-background: #0f0f23; -fx-text-fill: #00ff88; -fx-font-family: 'Monospaced';");

        getChildren().addAll(titleBar, txRow, pttButton, new Separator(),
                txMeter, rxMeter, new Separator(),
                new Label("Received:") {{ setTextFill(Color.LIGHTGRAY); }}, rxArea);
    }

    public void appendRx(String message) {
        rxArea.appendText(message + "\n");
    }

    public void setEnabled(boolean enabled) {
        transmitButton.setDisable(!enabled || voiceMode);
        txField.setDisable(!enabled || voiceMode);
        pttButton.setDisable(!enabled || !voiceMode);
    }

    public void setTxLevel(float level) {
        txLevel.setProgress(Math.min(1.0, level));
        txLevelLabel.setText(String.format("TX: %.0f%%", level * 100));
    }

    public void setRxLevel(float level) {
        rxLevel.setProgress(Math.min(1.0, level));
        rxLevelLabel.setText(String.format("RX: %.0f%%", level * 100));
    }

    public boolean isVoiceMode() {
        return voiceMode;
    }

    public void setOnTransmit(Consumer<String> handler) { this.onTransmit = handler; }
    public void setOnPttPress(Runnable handler) { this.onPttPress = handler; }
    public void setOnPttRelease(Runnable handler) { this.onPttRelease = handler; }
}
