package com.hamradio.dsp.blocks;

public class ToneGenerator extends SourceBlock {

    private final float frequency;
    private final float amplitude;
    private final int sampleRate;
    private long sampleIndex;

    public ToneGenerator(String id, float frequency, float amplitude, int sampleRate) {
        super(id);
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.sampleRate = sampleRate;
        this.sampleIndex = 0;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        for (int i = 0; i < numSamples; i++) {
            double t = (double) (sampleIndex + i) / sampleRate;
            output[i] = (float) (amplitude * Math.sin(2.0 * Math.PI * frequency * t));
        }
        sampleIndex += numSamples;
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }
}
