package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class IFFTBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final int fftSize;

    public IFFTBlock(String id, int fftSize) {
        super(id);
        this.fftSize = fftSize;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.fftInverse(input, fftSize);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return fftSize;
    }
}
