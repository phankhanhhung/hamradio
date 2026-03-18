package com.hamradio.ui;

import com.hamradio.net.Station;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class StationPanel extends VBox {

    private final Label callsignLabel = new Label();
    private final Label frequencyLabel = new Label();
    private final Label modeLabel = new Label();
    private final Label stateLabel = new Label();
    private final Label snrLabel = new Label("SNR: --");
    private final ProgressBar signalMeter = new ProgressBar(0);

    public StationPanel() {
        setSpacing(4);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #16213e; -fx-background-radius: 5;");

        callsignLabel.setFont(Font.font("Monospaced", 16));
        callsignLabel.setTextFill(Color.web("#00ff88"));
        frequencyLabel.setTextFill(Color.LIGHTGRAY);
        modeLabel.setTextFill(Color.LIGHTGRAY);
        stateLabel.setTextFill(Color.YELLOW);
        snrLabel.setTextFill(Color.web("#00ccff"));
        snrLabel.setFont(Font.font("Monospaced", 11));

        signalMeter.setPrefWidth(200);
        signalMeter.setPrefHeight(10);
        signalMeter.setStyle("-fx-accent: #00ff88;");

        getChildren().addAll(callsignLabel, frequencyLabel, modeLabel, stateLabel, snrLabel, signalMeter);
    }

    public void update(Station station) {
        if (station == null) return;
        callsignLabel.setText(station.getCallsign());
        frequencyLabel.setText(String.format("%.0f Hz", station.getFrequencyHz()));
        modeLabel.setText("Mode: " + station.getMode());
        stateLabel.setText("State: " + station.getTxRxState());
    }

    public void setTxRxState(String state) {
        stateLabel.setText("State: " + state);
        switch (state) {
            case "TRANSMITTING":
                stateLabel.setTextFill(Color.RED);
                break;
            case "RECEIVING":
                stateLabel.setTextFill(Color.web("#00ccff"));
                break;
            default:
                stateLabel.setTextFill(Color.YELLOW);
                break;
        }
    }

    public void setSNR(double snrDb) {
        snrLabel.setText(String.format("SNR: %.1f dB", snrDb));
        // Normalize SNR to 0-1 for meter (0 dB = 0, 60 dB = 1)
        double normalized = Math.max(0, Math.min(1, (snrDb + 20) / 80.0));
        signalMeter.setProgress(normalized);
        if (snrDb > 20) {
            signalMeter.setStyle("-fx-accent: #00ff88;");
        } else if (snrDb > 0) {
            signalMeter.setStyle("-fx-accent: #cccc00;");
        } else {
            signalMeter.setStyle("-fx-accent: #cc3333;");
        }
    }
}
