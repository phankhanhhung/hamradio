package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class AMDemodulatorBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final float carrierFreq;
    private final int sampleRate;

    public AMDemodulatorBlock(String id, float carrierFreq, int sampleRate) {
        super(id);
        this.carrierFreq = carrierFreq;
        this.sampleRate = sampleRate;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.demodulateAM(input, carrierFreq, sampleRate);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }
}
