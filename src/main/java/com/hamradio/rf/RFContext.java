package com.hamradio.rf;

public class RFContext {

    private final double frequencyHz;
    private final double distanceMeters;
    private final int sampleRate;
    private final double timeOfDay;   // 0-24 hours
    private final int season;         // 1=spring, 2=summer, 3=autumn, 4=winter
    private final double azimuth;     // degrees
    private final double elevation;   // degrees

    public RFContext(double frequencyHz, double distanceMeters, int sampleRate) {
        this(frequencyHz, distanceMeters, sampleRate, 12.0, 1, 0.0, 0.0);
    }

    public RFContext(double frequencyHz, double distanceMeters, int sampleRate,
                     double timeOfDay, int season, double azimuth, double elevation) {
        this.frequencyHz = frequencyHz;
        this.distanceMeters = distanceMeters;
        this.sampleRate = sampleRate;
        this.timeOfDay = timeOfDay;
        this.season = season;
        this.azimuth = azimuth;
        this.elevation = elevation;
    }

    public double getFrequencyHz() { return frequencyHz; }
    public double getDistanceMeters() { return distanceMeters; }
    public int getSampleRate() { return sampleRate; }
    public double getTimeOfDay() { return timeOfDay; }
    public int getSeason() { return season; }
    public double getAzimuth() { return azimuth; }
    public double getElevation() { return elevation; }
}
