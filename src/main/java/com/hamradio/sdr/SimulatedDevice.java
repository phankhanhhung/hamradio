package com.hamradio.sdr;

import java.util.Random;

public class SimulatedDevice implements SDRDevice {

    private boolean open;
    private int sampleRate = 44100;
    private double frequencyHz = 7100000;
    private float gainDb = 0;
    private final Random rng = new Random();

    @Override public String getName() { return "simulated"; }

    @Override
    public void open() { open = true; }

    @Override
    public void close() { open = false; }

    @Override
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }

    @Override
    public void setFrequency(double frequencyHz) { this.frequencyHz = frequencyHz; }

    @Override
    public void setGain(float gainDb) { this.gainDb = gainDb; }

    @Override
    public float[] readSamples(int count) {
        float[] samples = new float[count];
        float noiseLevel = (float) Math.pow(10.0, gainDb / 20.0) * 0.01f;
        for (int i = 0; i < count; i++) {
            // Gaussian noise
            samples[i] = (float) (rng.nextGaussian() * noiseLevel);
        }
        return samples;
    }

    @Override
    public void writeSamples(float[] samples) {
        // Simulated: no-op
    }

    @Override
    public boolean isOpen() { return open; }
}
