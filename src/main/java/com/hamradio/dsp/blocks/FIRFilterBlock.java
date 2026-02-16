package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class FIRFilterBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final float[] coefficients;
    private long handle;

    public FIRFilterBlock(String id, float[] coefficients) {
        super(id);
        this.coefficients = coefficients.clone();
    }

    @Override
    public void initialize() {
        handle = dsp.firCreate(coefficients, coefficients.length);
        if (handle == 0) {
            throw new RuntimeException("FIR filter creation failed");
        }
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.firProcess(handle, input);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }

    @Override
    public void dispose() {
        if (handle != 0) {
            dsp.firDestroy(handle);
            handle = 0;
        }
    }
}
