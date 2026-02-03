package com.hamradio.event.events;

import com.hamradio.event.Event;

public class SignalEvent extends Event {
    private final float[] samples;
    private final double frequency;
    private final int sampleRate;

    public SignalEvent(Object source, float[] samples, double frequency, int sampleRate) {
        super("signal", source);
        this.samples = samples;
        this.frequency = frequency;
        this.sampleRate = sampleRate;
    }

    public float[] getSamples() { return samples; }
    public double getFrequency() { return frequency; }
    public int getSampleRate() { return sampleRate; }
}
