package com.hamradio.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class StatusBar extends HBox {

    private final Label stateLabel = new Label("DRAFT");
    private final Label infoLabel = new Label("");

    public StatusBar() {
        setSpacing(20);
        setPadding(new Insets(5, 10, 5, 10));
        setStyle("-fx-background-color: #0a0a1a;");

        stateLabel.setFont(Font.font("Monospaced", 12));
        stateLabel.setTextFill(Color.YELLOW);

        infoLabel.setFont(Font.font("Monospaced", 11));
        infoLabel.setTextFill(Color.LIGHTGRAY);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(stateLabel, spacer, infoLabel);
    }

    public void setState(String state) {
        stateLabel.setText("State: " + state);
        switch (state) {
            case "RUNNING": stateLabel.setTextFill(Color.web("#00ff88")); break;
            case "PAUSED":  stateLabel.setTextFill(Color.YELLOW); break;
            case "FAILED":  stateLabel.setTextFill(Color.RED); break;
            default:        stateLabel.setTextFill(Color.LIGHTGRAY); break;
        }
    }

    public void setInfo(String info) {
        infoLabel.setText(info);
    }
}
