package com.hamradio.rf.models;

import com.hamradio.rf.PropagationModel;
import com.hamradio.rf.RFContext;

public class AntennaPatternModel implements PropagationModel {

    @Override
    public String getName() { return "antenna"; }

    @Override
    public float[] apply(float[] signal, RFContext context) {
        // Phase 1: isotropic antenna (no gain/loss)
        return signal.clone();
    }
}
