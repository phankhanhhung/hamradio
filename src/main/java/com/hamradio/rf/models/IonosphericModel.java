package com.hamradio.rf.models;

import com.hamradio.rf.NativeRF;
import com.hamradio.rf.PropagationModel;
import com.hamradio.rf.RFContext;

public class IonosphericModel implements PropagationModel {

    private final NativeRF nativeRF = new NativeRF();
    private final float criticalFreqMHz;
    private final float mufMHz;

    public IonosphericModel(float criticalFreqMHz, float mufMHz) {
        this.criticalFreqMHz = criticalFreqMHz;
        this.mufMHz = mufMHz;
    }

    public static IonosphericModel createDefault() {
        return new IonosphericModel(5.0f, 30.0f);
    }

    @Override
    public String getName() { return "ionospheric"; }

    @Override
    public float[] apply(float[] signal, RFContext context) {
        return nativeRF.applyIonosphericFading(signal, criticalFreqMHz, mufMHz,
                context.getSampleRate());
    }
}
