package com.hamradio.rf;

public interface ChannelModel {
    float[] process(float[] signal, RFContext context);
}
