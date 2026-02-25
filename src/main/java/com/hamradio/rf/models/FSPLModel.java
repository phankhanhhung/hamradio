package com.hamradio.rf.models;

import com.hamradio.rf.NativeRF;
import com.hamradio.rf.PropagationModel;
import com.hamradio.rf.RFContext;

public class FSPLModel implements PropagationModel {

    private final NativeRF nativeRF = new NativeRF();

    @Override
    public String getName() { return "fspl"; }

    @Override
    public float[] apply(float[] signal, RFContext context) {
        float lossDb = nativeRF.computeFSPL(
                (float) context.getFrequencyHz(),
                (float) context.getDistanceMeters()
        );
        float linearGain = (float) Math.pow(10.0, -lossDb / 20.0);
        float[] output = new float[signal.length];
        for (int i = 0; i < signal.length; i++) {
            output[i] = signal[i] * linearGain;
        }
        return output;
    }
}
