package com.hamradio.client;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Real-time microphone input — captures audio from the default mic
 * and delivers float[] chunks via a blocking queue.
 */
public class MicrophoneInput {

    private final int sampleRate;
    private final int chunkSize;
    private TargetDataLine micLine;
    private Thread captureThread;
    private volatile boolean running;
    private final BlockingQueue<float[]> audioQueue = new LinkedBlockingQueue<>(50);
    private volatile float peakLevel; // 0.0 - 1.0 for level meter

    public MicrophoneInput(int sampleRate, int chunkSize) {
        this.sampleRate = sampleRate;
        this.chunkSize = chunkSize;
    }

    public void start() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }

        micLine = (TargetDataLine) AudioSystem.getLine(info);
        micLine.open(format, chunkSize * 4); // buffer = 2x chunk in bytes
        micLine.start();
        running = true;

        captureThread = new Thread(this::captureLoop, "mic-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureLoop() {
        byte[] buffer = new byte[chunkSize * 2]; // 16-bit = 2 bytes per sample

        while (running) {
            int bytesRead = micLine.read(buffer, 0, buffer.length);
            if (bytesRead <= 0) continue;

            int samplesRead = bytesRead / 2;
            float[] samples = new float[samplesRead];
            float peak = 0;

            for (int i = 0; i < samplesRead; i++) {
                // Little-endian 16-bit signed → float [-1, 1]
                int lo = buffer[i * 2] & 0xFF;
                int hi = buffer[i * 2 + 1];
                short s = (short) (lo | (hi << 8));
                samples[i] = s / 32768.0f;
                float abs = Math.abs(samples[i]);
                if (abs > peak) peak = abs;
            }

            peakLevel = peak;
            audioQueue.offer(samples); // drop oldest if full
        }
    }

    public float[] poll() {
        return audioQueue.poll();
    }

    public float[] take() throws InterruptedException {
        return audioQueue.take();
    }

    public int available() {
        return audioQueue.size();
    }

    public float getPeakLevel() {
        return peakLevel;
    }

    public void stop() {
        running = false;
        if (captureThread != null) {
            captureThread.interrupt();
            try { captureThread.join(1000); } catch (InterruptedException ignored) {}
        }
        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }
        audioQueue.clear();
    }

    public boolean isRunning() {
        return running;
    }
}
