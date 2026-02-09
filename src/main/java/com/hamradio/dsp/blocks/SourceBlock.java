package com.hamradio.dsp.blocks;

import com.hamradio.dsp.graph.DSPBlock;

public abstract class SourceBlock extends DSPBlock {

    protected SourceBlock(String id) {
        super(id);
        addOutput("out");
    }

    @Override
    public abstract void process(float[] input, float[] output, int numSamples);
}
