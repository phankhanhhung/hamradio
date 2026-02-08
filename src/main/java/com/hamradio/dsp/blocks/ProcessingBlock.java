package com.hamradio.dsp.blocks;

import com.hamradio.dsp.graph.DSPBlock;

public abstract class ProcessingBlock extends DSPBlock {

    protected ProcessingBlock(String id) {
        super(id);
        addInput("in");
        addOutput("out");
    }

    @Override
    public abstract void process(float[] input, float[] output, int numSamples);
}
