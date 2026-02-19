package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class FMDemodulatorBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final float carrierFreq;
    private final float deviation;
    private final int sampleRate;

    public FMDemodulatorBlock(String id, float carrierFreq, float deviation, int sampleRate) {
        super(id);
        this.carrierFreq = carrierFreq;
        this.deviation = deviation;
        this.sampleRate = sampleRate;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.demodulateFM(input, carrierFreq, deviation, sampleRate);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }
}
