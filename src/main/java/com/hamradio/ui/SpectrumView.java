package com.hamradio.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SpectrumView extends Canvas {

    private float[] magnitudes;
    private final Color backgroundColor = Color.web("#0f0f23");
    private final Color lineColor = Color.web("#00ff88");
    private final Color gridColor = Color.web("#1a1a3e");

    public SpectrumView(double width, double height) {
        super(width, height);
        drawBackground();
    }

    public void update(float[] magnitudes) {
        this.magnitudes = magnitudes;
        draw();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // Background
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, w, h);

        // Grid
        gc.setStroke(gridColor);
        gc.setLineWidth(0.5);
        for (int i = 1; i < 10; i++) {
            double y = h * i / 10;
            gc.strokeLine(0, y, w, y);
        }
        for (int i = 1; i < 10; i++) {
            double x = w * i / 10;
            gc.strokeLine(x, 0, x, h);
        }

        if (magnitudes == null || magnitudes.length == 0) return;

        // Find max for normalization
        float max = 0;
        int bins = magnitudes.length / 2; // show only first half (Nyquist)
        for (int i = 0; i < bins; i++) {
            if (magnitudes[i] > max) max = magnitudes[i];
        }
        if (max == 0) max = 1;

        // Draw spectrum line
        gc.setStroke(lineColor);
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < bins; i++) {
            double x = (double) i / bins * w;
            double y = h - (magnitudes[i] / max) * h * 0.9;
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();

        // Labels
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Spectrum Analyzer", 10, 15);
        gc.fillText("Bins: " + bins, w - 80, 15);
    }

    private void drawBackground() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.GRAY);
        gc.fillText("No data", getWidth() / 2 - 20, getHeight() / 2);
    }
}
