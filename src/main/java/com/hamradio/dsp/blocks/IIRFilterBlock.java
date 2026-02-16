package com.hamradio.dsp.blocks;

import com.hamradio.dsp.NativeDSP;

public class IIRFilterBlock extends ProcessingBlock {

    private final NativeDSP dsp = new NativeDSP();
    private final float[] bCoeffs;
    private final float[] aCoeffs;
    private long handle;

    public IIRFilterBlock(String id, float[] bCoeffs, float[] aCoeffs) {
        super(id);
        this.bCoeffs = bCoeffs.clone();
        this.aCoeffs = aCoeffs.clone();
    }

    @Override
    public void initialize() {
        handle = dsp.iirCreate(bCoeffs, aCoeffs);
        if (handle == 0) {
            throw new RuntimeException("IIR filter creation failed");
        }
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = dsp.iirProcess(handle, input);
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
            dsp.iirDestroy(handle);
            handle = 0;
        }
    }
}
