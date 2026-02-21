package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class SSBDemodulatorBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final float carrierFreq;
    private final int sampleRate;
    private final boolean upperSideband;

    public SSBDemodulatorBlock(String id, float carrierFreq, int sampleRate, boolean upperSideband) {
        super(id);
        this.carrierFreq = carrierFreq;
        this.sampleRate = sampleRate;
        this.upperSideband = upperSideband;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.demodulateSSB(input, carrierFreq, sampleRate, upperSideband);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }
}
