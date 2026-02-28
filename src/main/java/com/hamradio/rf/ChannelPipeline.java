package com.hamradio.rf;

import java.util.ArrayList;
import java.util.List;

public class ChannelPipeline implements ChannelModel {

    private final List<PropagationModel> stages = new ArrayList<>();

    public void addStage(PropagationModel model) {
        stages.add(model);
    }


    @Override
    public float[] process(float[] signal, RFContext context) {
        float[] current = signal;
        for (PropagationModel stage : stages) {
            current = stage.apply(current, context);
        }
        return current;
    }

    public List<PropagationModel> getStages() {
        return new ArrayList<>(stages);
    }
}
