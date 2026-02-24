package com.hamradio.rf;

public class RFContext {

    private final double frequencyHz;
    private final double distanceMeters;
    private final int sampleRate;

    public RFContext(double frequencyHz, double distanceMeters, int sampleRate) {
        this.frequencyHz = frequencyHz;
        this.distanceMeters = distanceMeters;
        this.sampleRate = sampleRate;
    }

    public double getFrequencyHz() { return frequencyHz; }
    public double getDistanceMeters() { return distanceMeters; }
    public int getSampleRate() { return sampleRate; }
}
