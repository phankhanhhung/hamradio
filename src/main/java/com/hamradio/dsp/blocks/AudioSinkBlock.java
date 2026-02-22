package com.hamradio.dsp.blocks;

import javax.sound.sampled.*;

public class AudioSinkBlock extends SinkBlock {

    private final int sampleRate;
    private SourceDataLine audioLine;

    public AudioSinkBlock(String id, int sampleRate) {
        super(id);
        this.sampleRate = sampleRate;
    }

    @Override
    public void initialize() {
        try {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format, sampleRate);
            audioLine.start();
        } catch (LineUnavailableException e) {
            System.err.println("[AudioSink] Audio line unavailable: " + e.getMessage());
            audioLine = null;
        }
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        if (audioLine == null || input == null) return;

        byte[] bytes = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, input[i]));
            short sample = (short) (clamped * 32767);
            bytes[i * 2] = (byte) (sample & 0xFF);
            bytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        audioLine.write(bytes, 0, bytes.length);
    }

    @Override
    public void dispose() {
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
    }
}
