package com.hamradio.dsp;

public class NativeDSP {

    static {
        System.loadLibrary("hamradio");
    }

    // Lifecycle
    public native int dspInit(int sampleRate);
    public native void dspShutdown();

    // FFT
    public native float[] fftForward(float[] realSamples, int size);
    public native float[] fftInverse(float[] freqDomain, int size);

    // FIR Filter (handle-based)
    public native long firCreate(float[] coefficients, int numTaps);
    public native float[] firProcess(long handle, float[] input);
    public native void firDestroy(long handle);

    // IIR Filter (handle-based)
    public native long iirCreate(float[] bCoeffs, float[] aCoeffs);
    public native float[] iirProcess(long handle, float[] input);
    public native void iirDestroy(long handle);

    // AM Modulation
    public native float[] modulateAM(float[] baseband, float carrierFreq, int sampleRate);
    public native float[] demodulateAM(float[] signal, float carrierFreq, int sampleRate);

    // FM Modulation
    public native float[] modulateFM(float[] baseband, float carrierFreq, float deviation, int sampleRate);
    public native float[] demodulateFM(float[] signal, float carrierFreq, float deviation, int sampleRate);

    // SSB Modulation
    public native float[] modulateSSB(float[] baseband, float carrierFreq, int sampleRate, boolean upperSideband);
    public native float[] demodulateSSB(float[] signal, float carrierFreq, int sampleRate, boolean upperSideband);

    // Resampling
    public native float[] resample(float[] input, int inputRate, int outputRate);
}
