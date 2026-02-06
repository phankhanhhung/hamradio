package com.hamradio.dsp.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class DSPBlock {

    private final String id;
    private final List<Port> inputs = new ArrayList<>();
    private final List<Port> outputs = new ArrayList<>();

    protected DSPBlock(String id) {
        this.id = id;
    }

    protected Port addInput(String name) {
        Port p = new Port(name, Port.Direction.INPUT);
        p.setOwner(this);
        inputs.add(p);
        return p;
    }

    protected Port addOutput(String name) {
        Port p = new Port(name, Port.Direction.OUTPUT);
        p.setOwner(this);
        outputs.add(p);
        return p;
    }

    public abstract void process(float[] input, float[] output, int numSamples);

    public abstract int getOutputSize(int inputSize);



    public String getId() { return id; }
    public List<Port> getInputs() { return Collections.unmodifiableList(inputs); }
    public List<Port> getOutputs() { return Collections.unmodifiableList(outputs); }

    public Port getInput(String name) {
        return inputs.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    public Port getOutput(String name) {
        return outputs.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }
}
