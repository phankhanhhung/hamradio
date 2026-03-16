package com.hamradio.sdr;

public interface SDRDevice {
    String getName();
    void open();
    void close();
    void setSampleRate(int sampleRate);
    void setFrequency(double frequencyHz);
    void setGain(float gainDb);
    float[] readSamples(int count);
    void writeSamples(float[] samples);
    boolean isOpen();
}
