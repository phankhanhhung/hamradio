package com.hamradio.ui;

import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class LogPanel extends VBox {

    private final TextArea logArea = new TextArea();

    public LogPanel() {
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        logArea.setStyle("-fx-control-inner-background: #0f0f23; -fx-text-fill: #00ff88; -fx-font-family: 'Monospaced'; -fx-font-size: 11;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        getChildren().add(logArea);
    }

    public void append(String message) {
        logArea.appendText(message + "\n");
    }

    public void clear() {
        logArea.clear();
    }
}
