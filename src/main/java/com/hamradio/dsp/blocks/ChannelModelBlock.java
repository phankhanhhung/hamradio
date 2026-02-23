package com.hamradio.dsp.blocks;

import com.hamradio.rf.ChannelModel;
import com.hamradio.rf.RFContext;

public class ChannelModelBlock extends ProcessingBlock {

    private final ChannelModel channelModel;
    private final RFContext context;

    public ChannelModelBlock(String id, ChannelModel channelModel, RFContext context) {
        super(id);
        this.channelModel = channelModel;
        this.context = context;
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        float[] result = channelModel.process(input, context);
        if (result != null) {
            System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }
}
