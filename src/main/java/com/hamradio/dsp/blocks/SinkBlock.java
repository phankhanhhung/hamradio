package com.hamradio.dsp.blocks;

import com.hamradio.dsp.graph.DSPBlock;

public abstract class SinkBlock extends DSPBlock {

    protected SinkBlock(String id) {
        super(id);
        addInput("in");
    }

    @Override
    public int getOutputSize(int inputSize) {
        return 0;
    }

    @Override
    public abstract void process(float[] input, float[] output, int numSamples);
}
