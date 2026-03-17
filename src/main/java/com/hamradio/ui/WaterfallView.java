package com.hamradio.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class WaterfallView extends Canvas {

    private WritableImage image;
    private int scrollY;
    private final Color backgroundColor = Color.web("#0f0f23");

    public WaterfallView(double width, double height) {
        super(width, height);
        image = new WritableImage((int) width, (int) height);
        scrollY = 0;
        drawBackground();
    }

    public void addLine(float[] magnitudes) {
        if (magnitudes == null || magnitudes.length == 0) return;

        int w = (int) getWidth();
        int h = (int) getHeight();
        PixelWriter pw = image.getPixelWriter();

        // Find max for normalization
        float max = 0;
        int bins = magnitudes.length / 2;
        for (int i = 0; i < bins; i++) {
            if (magnitudes[i] > max) max = magnitudes[i];
        }
        if (max == 0) max = 1;

        // Shift image down by 1 pixel
        WritableImage newImage = new WritableImage(w, h);
        PixelWriter npw = newImage.getPixelWriter();

        // Copy old image shifted down
        for (int y = 1; y < h; y++) {
            for (int x = 0; x < w; x++) {
                npw.setColor(x, y, image.getPixelReader().getColor(x, y - 1));
            }
        }

        // Draw new line at top
        for (int x = 0; x < w; x++) {
            int bin = (int) ((double) x / w * bins);
            if (bin >= bins) bin = bins - 1;
            float normalized = magnitudes[bin] / max;
            Color color = magnitudeToColor(normalized);
            npw.setColor(x, 0, color);
        }

        image = newImage;

        // Draw to canvas
        GraphicsContext gc = getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
    }

    private Color magnitudeToColor(float value) {
        value = Math.max(0, Math.min(1, value));
        if (value < 0.25f) {
            return Color.color(0, 0, value * 4);           // black -> blue
        } else if (value < 0.5f) {
            return Color.color(0, (value - 0.25f) * 4, 1); // blue -> cyan
        } else if (value < 0.75f) {
            return Color.color((value - 0.5f) * 4, 1, 1 - (value - 0.5f) * 4); // cyan -> yellow
        } else {
            return Color.color(1, 1 - (value - 0.75f) * 4, 0); // yellow -> red
        }
    }

    private void drawBackground() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.GRAY);
        gc.fillText("Waterfall - No data", getWidth() / 2 - 50, getHeight() / 2);
    }
}
