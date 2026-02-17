package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class FFTBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final int fftSize;

    public FFTBlock(String id, int fftSize) {
        super(id);
        if ((fftSize & (fftSize - 1)) != 0) {
            throw new IllegalArgumentException("FFT size must be power of 2: " + fftSize);
        }
        this.fftSize = fftSize;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] padded = input;
        if (numSamples < fftSize) {
            padded = new float[fftSize];
            System.arraycopy(input, 0, padded, 0, numSamples);
        }
        float[] result = dsp.fftForward(padded, fftSize);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return fftSize * 2; // interleaved magnitude + phase
    }

    public int getFFTSize() { return fftSize; }
}
