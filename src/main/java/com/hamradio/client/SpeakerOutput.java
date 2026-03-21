package com.hamradio.client;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Real-time speaker output — plays received audio through the default speaker.
 * Accepts float[] chunks via a queue, converts to PCM, writes to SourceDataLine.
 */
public class SpeakerOutput {

    private final int sampleRate;
    private SourceDataLine speakerLine;
    private Thread playbackThread;
    private volatile boolean running;
    private final BlockingQueue<float[]> audioQueue = new LinkedBlockingQueue<>(100);
    private volatile float peakLevel;

    public SpeakerOutput(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void start() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        speakerLine = (SourceDataLine) AudioSystem.getLine(info);
        speakerLine.open(format, sampleRate / 5); // ~200ms buffer
        speakerLine.start();
        running = true;

        playbackThread = new Thread(this::playbackLoop, "speaker-output");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void playbackLoop() {
        while (running) {
            try {
                float[] samples = audioQueue.take();
                writeSamples(samples);
            } catch (InterruptedException e) {
                if (!running) break;
            }
        }
    }

    private void writeSamples(float[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        float peak = 0;

        for (int i = 0; i < samples.length; i++) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, samples[i]));
            short s = (short) (clamped * 32767);
            bytes[i * 2] = (byte) (s & 0xFF);
            bytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            float abs = Math.abs(clamped);
            if (abs > peak) peak = abs;
        }

        peakLevel = peak;
        speakerLine.write(bytes, 0, bytes.length);
    }

    public void play(float[] samples) {
        audioQueue.offer(samples);
    }

    public float getPeakLevel() {
        return peakLevel;
    }

    public int getQueueSize() {
        return audioQueue.size();
    }

    public void stop() {
        running = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
            try { playbackThread.join(1000); } catch (InterruptedException ignored) {}
        }
        if (speakerLine != null) {
            speakerLine.drain();
            speakerLine.stop();
            speakerLine.close();
        }
        audioQueue.clear();
    }

    public boolean isRunning() {
        return running;
    }
}
