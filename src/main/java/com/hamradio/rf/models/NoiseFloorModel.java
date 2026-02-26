package com.hamradio.rf.models;

import com.hamradio.rf.NativeRF;
import com.hamradio.rf.PropagationModel;
import com.hamradio.rf.RFContext;

public class NoiseFloorModel implements PropagationModel {

    private final NativeRF nativeRF = new NativeRF();
    private final float noisePowerDbm;

    public NoiseFloorModel(float noisePowerDbm) {
        this.noisePowerDbm = noisePowerDbm;
    }

    public static NoiseFloorModel createDefault() {
        return new NoiseFloorModel(-100.0f);
    }

    @Override
    public String getName() { return "noise_floor"; }

    @Override
    public float[] apply(float[] signal, RFContext context) {
        return nativeRF.addNoiseFloor(signal, noisePowerDbm, context.getSampleRate());
    }
}
