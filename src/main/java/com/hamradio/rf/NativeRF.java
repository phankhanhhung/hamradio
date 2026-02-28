package com.hamradio.rf;

public class NativeRF {

    static {
        System.loadLibrary("hamradio");
    }

    public native float computeFSPL(float frequencyHz, float distanceMeters);
    public native float[] applyMultipath(float[] signal, int numPaths, float[] delaysSec, float[] amplitudes, float[] phaseOffsets, int sampleRate);
    public native float[] applyIonosphericFading(float[] signal, float criticalFreqMHz, float maxUsableFreqMHz, int sampleRate);
    public native float[] addNoiseFloor(float[] signal, float noisePowerDbm, int sampleRate);
    public native float computeAntennaGain(float azimuth, float elevation, float[] pattern);
}
