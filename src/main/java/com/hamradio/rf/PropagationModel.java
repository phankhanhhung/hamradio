package com.hamradio.rf;

public interface PropagationModel {
    String getName();
    float[] apply(float[] signal, RFContext context);
}
