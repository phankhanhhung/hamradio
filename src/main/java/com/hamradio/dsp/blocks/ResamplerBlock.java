package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class ResamplerBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final int inputRate;
    private final int outputRate;

    public ResamplerBlock(String id, int inputRate, int outputRate) {
        super(id);
        this.inputRate = inputRate;
        this.outputRate = outputRate;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.resample(input, inputRate, outputRate);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return (int) ((long) inputSize * outputRate / inputRate) + 1;
    }
}
