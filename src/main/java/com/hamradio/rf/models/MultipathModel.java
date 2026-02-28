package com.hamradio.rf.models;

import com.hamradio.rf.NativeRF;
import com.hamradio.rf.PropagationModel;
import com.hamradio.rf.RFContext;

public class MultipathModel implements PropagationModel {

    private final NativeRF nativeRF = new NativeRF();
    private final int numPaths;
    private final float[] delays;
    private final float[] amplitudes;
    private final float[] phaseOffsets;

    public MultipathModel(int numPaths, float[] delays, float[] amplitudes, float[] phaseOffsets) {
        this.numPaths = numPaths;
        this.delays = delays.clone();
        this.amplitudes = amplitudes.clone();
        this.phaseOffsets = phaseOffsets.clone();
    }

    public static MultipathModel createDefault() {
        return new MultipathModel(3,
                new float[]{0.0f, 0.001f, 0.003f},
                new float[]{1.0f, 0.5f, 0.25f},
                new float[]{0.0f, 0.5f, 1.2f}
        );
    }

    @Override
    public String getName() { return "multipath"; }

    @Override
    public float[] apply(float[] signal, RFContext context) {
        return nativeRF.applyMultipath(signal, numPaths, delays, amplitudes,
                phaseOffsets, context.getSampleRate());
    }
}
